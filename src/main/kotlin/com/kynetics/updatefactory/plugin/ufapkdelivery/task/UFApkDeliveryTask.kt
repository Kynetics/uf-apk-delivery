/*
 *
 *  Copyright (c) 2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 */
package com.kynetics.updatefactory.plugin.ufapkdelivery.task

import com.android.build.gradle.AppExtension
import com.google.gson.Gson
import com.kynetics.updatefactory.plugin.ufapkdelivery.Plugin.Companion.EXTENSION_NAME
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Client
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Distribution
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Id
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.SoftwareModule
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtension
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtensionInterface
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UfApkDeliverPlugExtensionMerge
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import retrofit2.Response
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Daniele Sergio
 */

@CacheableTask
open class UFApkDeliveryTask : AbstractTask() {
    companion object {
        val gson = Gson()
    }

    @TaskAction/**/
    @Suppress("unused")
    fun action() {
        val defaultExtension = UFApkDeliveryPluginExtension()
        defaultExtension.softwareModuleType="application"
        defaultExtension.distributionType="app"
        defaultExtension.buildType="release"
        val extension = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(project, EXTENSION_NAME)!!, defaultExtension)
        val client = Client(extension.buildUrl())
        val basic = Credentials.basic("${extension.tenant}\\${extension.username}", extension.password)
        val softwareModulesCreated = buildSoftwareModuleByDeliveryType(extension,client, basic)
        if(softwareModulesCreated.isEmpty()){
            return
        }
        buildDistributionByDeliveryType(extension,client,basic, softwareModulesCreated)

    }

    private fun buildDistributionByDeliveryType(extension: UFApkDeliveryPluginExtensionInterface,
                                                client: Client,
                                                basic:String,
                                                softwareModulesCreated:Map<Id, SoftwareModule>){
        when (extension.deliveryType){
            UFApkDeliveryPluginExtensionInterface.DeliveryType.MULTIPLE_DISTRIBUTION ->{
                val distributionCreated = mutableMapOf<Id, Distribution>()
                val softwareModuleIterator = softwareModulesCreated.iterator()
                project.subprojects.filterNot{ p -> extension.ignoreProjects.contains(p.name) }
                        .forEach{
                            val android = getExtension<AppExtension>(it, "android")!!
                            val ext = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(it, EXTENSION_NAME)
                                    ?: extension, extension)
                            val distribution = Distribution(
                                    it.name,
                                    "",
                                    ext.distributionType,
                                    android.defaultConfig.versionName,
                                    setOf(softwareModuleIterator.next().key))

                            var responseCreateDistribution : Response<Array<Distribution>>? = null
                            try {
                                responseCreateDistribution = client.createDistribution(basic, listOf(distribution)).execute()
                            } catch (e:Exception){
                                logger.error(e.message, e)
                            }

                            if(onError(responseCreateDistribution, client, basic, softwareModulesCreated, distributionCreated)){
                                return
                            }

                            val newDistribution = responseCreateDistribution?.body()!![0]
                            distributionCreated[Id(newDistribution.id!!)] = newDistribution
                            logger.quiet("New distribution created: $newDistribution")

                        }
            }
            else ->{
                val distribution = Distribution(
                        project.name,
                        "",
                        extension.distributionType,
                        extension.version,
                        softwareModulesCreated.keys)

                var responseCreateDistribution : Response<Array<Distribution>>? = null
                try {
                    responseCreateDistribution = client.createDistribution(basic, listOf(distribution)).execute()
                } catch (e:Exception){
                    logger.error(e.message, e)
                }

                if(!onError(responseCreateDistribution, client, basic, softwareModulesCreated, emptyMap())){
                    val newDistribution = responseCreateDistribution?.body()!![0]
                    logger.quiet("New distribution created: $newDistribution")
                }
            }
        }
    }

    private fun buildSoftwareModuleByDeliveryType(extension: UFApkDeliveryPluginExtensionInterface, client: Client, basic:String) : Map<Id, SoftwareModule>{
        val softwareModulesCreated = mutableMapOf<Id, SoftwareModule>()

        return when(extension.deliveryType){

            UFApkDeliveryPluginExtensionInterface.DeliveryType.SINGLE_SOFTWARE_MODULE ->{

                val softwareModule = SoftwareModule(extension.vendor,
                        project.name,
                        "",
                        extension.softwareModuleType,
                        extension.version)

                var response : Response<Array<SoftwareModule>>? = null
                try {
                    response = client.createSoftwareModule(basic, arrayOf(softwareModule)).execute()
                } catch (e:Exception){
                    logger.error(e.message, e)
                }

                if(response == null || !response.isSuccessful) {
                    onError(response, client, basic, softwareModulesCreated, emptyMap())
                    return emptyMap()
                }

                val newSoftwareModule = response.body()!![0]
                logger.quiet("New software module created: $newSoftwareModule")
                softwareModulesCreated[Id(newSoftwareModule.id!!)] = newSoftwareModule

                project.subprojects.filterNot{ p ->  extension.ignoreProjects.contains(p.name) }
                        .forEach{
                            val ext = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(it, EXTENSION_NAME)
                                    ?: extension, extension)
                            val files = getDirectoryOutputFiles(it, ext.buildType, ext.productFlavor)
                            if(files.isEmpty()){
                                return clearIdentifiableList(softwareModulesCreated, client::deleteSoftwareModule, basic)
                            }
                            files.forEach { file ->
                                val requestFile = RequestBody.create(MediaType.parse(Files.probeContentType(Paths.get(file.absolutePath))),file)
                                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                                var responseUpload : Response<Any>? = null
                                try {
                                    responseUpload = client.uploadFile(basic, newSoftwareModule.id, filePart).execute()
                                } catch (e:Exception){
                                    logger.error(e.message, e)
                                }

                                if(onError(responseUpload, client, basic, softwareModulesCreated, emptyMap())){
                                    return emptyMap() //todo test return inside foreach
                                }
                                logger.quiet("File(${file.name} successfully uploaded to Update Factory server into ${softwareModule.name} software module")
                            }
                        }

                softwareModulesCreated
            }

            else ->{
                project.subprojects.filterNot{ p -> extension.ignoreProjects.contains(p.name) }
                        .forEach{
                            val android = getExtension<AppExtension>(it, "android")!!

                            val ext = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(it, EXTENSION_NAME)
                                    ?: extension, extension)

                            val nameSuffix = if(ext.productFlavor.isEmpty()) ext.buildType else "${ext.productFlavor}-${ext.buildType}"
                            val softwareModule = SoftwareModule(ext.vendor,
                                    "${it.name}-$nameSuffix",
                                    "",
                                    ext.softwareModuleType,
                                    android.defaultConfig.versionName)

                            var response : Response<Array<SoftwareModule>>? = null
                            try {
                                response = client.createSoftwareModule(basic, arrayOf(softwareModule)).execute()
                            } catch (e:Exception){
                                logger.error(e.message, e)
                            }

                            if(response == null || response?.isSuccessful == false ) {
                                onError(response, client, basic, softwareModulesCreated, emptyMap())
                                return emptyMap()
                            }

                            val newSoftwareModule = response!!.body()!![0]
                            logger.quiet("New software module created: $newSoftwareModule")
                            softwareModulesCreated[Id(newSoftwareModule.id!!)] = newSoftwareModule

                            val files = getDirectoryOutputFiles(it, ext.buildType, ext.productFlavor)
                            if(files.isEmpty()){
                                return clearIdentifiableList(softwareModulesCreated, client::deleteSoftwareModule, basic) //todo test return inside foreach
                            }
                            files.forEach { file ->
                                val requestFile = RequestBody.create(MediaType.parse(Files.probeContentType(Paths.get(file.absolutePath))),file)
                                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                                var responseUpload : Response<Any>? = null
                                try {
                                    responseUpload = client.uploadFile(basic, newSoftwareModule.id, filePart).execute()
                                } catch (e:Exception){
                                    logger.error(e.message, e)
                                }

                                if(onError(responseUpload, client, basic, softwareModulesCreated, emptyMap())){
                                    return emptyMap() //todo test return inside foreach
                                }
                                logger.quiet("File(${file.name} successfully uploaded to Update Factory server into ${softwareModule.name} software module")
                            }
                        }

                softwareModulesCreated
            }

        }
    }

}