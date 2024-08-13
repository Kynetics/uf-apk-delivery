/*
 *
 *  Copyright (c) 2024 Kynetics, Inc.
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
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Distribution
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Id
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.ManagementApi
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.SoftwareModule
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.getManagementApiClient
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtension
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtensionInterface
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UfApkDeliverPlugExtensionMerge
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import retrofit2.Response
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

/**
 * @author Daniele Sergio
 */

@CacheableTask
open class UFApkDeliveryTask : AbstractTask() {
    companion object {
        val gson = Gson()
    }

    @TaskAction
    @Suppress("unused")
    fun action() {
        val defaultExtension = UFApkDeliveryPluginExtension()
        defaultExtension.softwareModuleType="application"
        defaultExtension.distributionType="app"
        defaultExtension.buildType="release"
        defaultExtension.softwareModuleName= project.name
        val extension = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(project, EXTENSION_NAME)!!, defaultExtension)
        val client = getManagementApiClient(extension.buildUrl())
        val basic = Credentials.basic("${extension.tenant}\\${extension.username}", extension.password)
        val softwareModulesCreated = buildSoftwareModuleByDeliveryType(extension,client, basic)
        if(softwareModulesCreated.isEmpty()){
            return
        }
        buildDistributionByDeliveryType(extension,client,basic, softwareModulesCreated)

    }

    private fun buildDistributionByDeliveryType(extension: UFApkDeliveryPluginExtensionInterface,
                                                client: ManagementApi,
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
                            val softwareModuleName = ext.softwareModuleName.ifEmpty { "${extension.softwareModuleName}-${it.name}" }
                            val distribution = Distribution(
                                softwareModuleName,
                                    "",
                                    ext.distributionType,
                                    android.defaultConfig.versionName?: "",
                                    setOf(softwareModuleIterator.next().key))

                            val newDistribution = createDistributionOnTheServerAndHandleTheResponse(
                                client, basic, distribution, distributionCreated, softwareModulesCreated)
                            if (newDistribution != null) {
                                distributionCreated[Id(newDistribution.id!!)] = newDistribution
                                logger.quiet("New distribution created: $newDistribution")
                            }

                        }
            }
            else ->{
                val distribution = Distribution(
                        extension.softwareModuleName,
                        "",
                        extension.distributionType,
                        extension.version,
                        softwareModulesCreated.keys)

                val newDistribution = createDistributionOnTheServerAndHandleTheResponse(client, basic,
                    distribution, emptyMap(), softwareModulesCreated)
                if (newDistribution != null) {
                    logger.quiet("New distribution created: $newDistribution")
                }
            }
        }
    }

    private fun buildSoftwareModuleByDeliveryType(extension: UFApkDeliveryPluginExtensionInterface, client: ManagementApi, basic:String) : Map<Id, SoftwareModule>{
        val softwareModulesCreated = mutableMapOf<Id, SoftwareModule>()

        return when(extension.deliveryType){

            UFApkDeliveryPluginExtensionInterface.DeliveryType.SINGLE_SOFTWARE_MODULE ->{

                val softwareModule = SoftwareModule(extension.vendor,
                        extension.softwareModuleName,
                        "",
                        extension.softwareModuleType,
                        extension.version)


                val newSoftwareModule: SoftwareModule =
                    createSoftwareModuleAndHandleTheResponse(client, basic,
                        softwareModulesCreated, softwareModule)

                softwareModulesCreated[Id(newSoftwareModule.id!!)] = newSoftwareModule

                project.subprojects.filterNot{ p ->  extension.ignoreProjects.contains(p.name) }
                        .forEach{
                            val ext = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(it, EXTENSION_NAME)
                                    ?: extension, extension)

                            uploadApksToTheServer(ext, it, newSoftwareModule,
                                softwareModulesCreated, client, basic,
                                crashIFApksNotFound = true)
                        }

                softwareModulesCreated
            }

            else ->{
                project.subprojects.filterNot{ p -> extension.ignoreProjects.contains(p.name) }
                        .forEach{
                            val android = getExtension<AppExtension>(it, "android")!!

                            val ext = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(it, EXTENSION_NAME)
                                    ?: extension, extension)

                            val softwareModuleName = ext.softwareModuleName.ifEmpty { "${extension.softwareModuleName}-${it.name}" }
                            val nameSuffix = if(ext.productFlavor.isEmpty()) ext.buildType else "${ext.productFlavor}-${ext.buildType}"
                            val softwareModule = SoftwareModule(ext.vendor,
                                "$softwareModuleName-$nameSuffix",
                                    "",
                                    ext.softwareModuleType,
                                    android.defaultConfig.versionName?: "")

                            val newSoftwareModule: SoftwareModule =
                                createSoftwareModuleAndHandleTheResponse(client, basic,
                                    softwareModulesCreated, softwareModule)

                            softwareModulesCreated[Id(newSoftwareModule.id!!)] = newSoftwareModule

                            uploadApksToTheServer(ext, it, newSoftwareModule,
                                softwareModulesCreated, client, basic,
                                crashIFApksNotFound = false)
                        }

                softwareModulesCreated
            }

        }
    }

    private fun createDistributionOnTheServerAndHandleTheResponse(
        client: ManagementApi,
        basic: String,
        distribution: Distribution,
        distributionCreated: Map<Id, Distribution>,
        softwareModulesCreated: Map<Id, SoftwareModule>,
    ): Distribution? {

        var responseCreateDistribution: Response<Array<Distribution>>? = null
        try {
            responseCreateDistribution =
                client.createDistribution(basic, listOf(distribution)).execute()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

        if (responseCreateDistribution == null || !responseCreateDistribution.isSuccessful) {
            if (responseCreateDistribution?.code() == 409) {
                logger.quiet("Distribution ${distribution.name} already exists")
                return null
            } else {
                onError(responseCreateDistribution, client, basic, softwareModulesCreated,
                    distributionCreated)
                throw IOException(getErrorMessage(responseCreateDistribution))
            }
        }

        return responseCreateDistribution.body()!![0]
    }

    private fun createSoftwareModuleAndHandleTheResponse(
        client: ManagementApi,
        basic: String,
        softwareModulesCreated: Map<Id, SoftwareModule>,
        softwareModule: SoftwareModule): SoftwareModule {

        var response: Response<Array<SoftwareModule>>? = null
        try {
            response = client.createSoftwareModule(basic, arrayOf(softwareModule)).execute()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

        val newSoftwareModule: SoftwareModule
        if (response == null || !response.isSuccessful) {
            onError(response, client, basic, softwareModulesCreated, emptyMap())
            if (response?.code() == 409) {
                logger.quiet("Software module ${softwareModule.name} already exists, deleting artifacts")
                newSoftwareModule =
                    deleteAllTheArtifactsFromTheSoftwareModule(client, basic, softwareModule.name)
            } else {
                throw IOException(getErrorMessage(response))
            }
        } else {
            newSoftwareModule = response.body()!![0]
            logger.quiet("New software module created: $newSoftwareModule")
        }
        return newSoftwareModule
    }

    private fun deleteAllTheArtifactsFromTheSoftwareModule(client: ManagementApi,
                                                           basic: String,
                                                           softwareModuleName: String): SoftwareModule {
        val softwareModules = client.getSoftwareModules(basic).execute()
        val newSoftwareModule =
            softwareModules.body()?.content?.find { it.name == softwareModuleName }
                ?: throw IllegalStateException(
                    "Software module $softwareModuleName is deleted on the server! Please, change the name of the software module in the plugin configuration")
        val artifacts =
            client.getSoftwareModuleArtifacts(basic, newSoftwareModule.id!!).execute()
        artifacts.body()?.forEach { artifactId ->
            client.deleteArtifact(basic, newSoftwareModule.id, artifactId.id).execute()
        }
        return newSoftwareModule
    }

    private fun uploadApksToTheServer(
        ext: UfApkDeliverPlugExtensionMerge, project: Project,
        newSoftwareModule: SoftwareModule,
        softwareModulesCreated: Map<Id, SoftwareModule>,
        client: ManagementApi, basic: String,
        crashIFApksNotFound: Boolean = true) {

        val files = getDirectoryOutputFiles(project, ext.buildType, ext.productFlavor)
        if (files.isEmpty()) {
            clearIdentifiableList(softwareModulesCreated, client::deleteSoftwareModule, basic)
            if (crashIFApksNotFound)
                throw NoSuchFileException("No apk files found in ${project.name} module")
            else return
        }
        files.forEach { file ->
            val requestFile = RequestBody.create(
                MediaType.parse(Files.probeContentType(Paths.get(file.absolutePath))), file)
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            var responseUpload: Response<Any>? = null
            try {
                responseUpload =
                    client.uploadFile(basic, newSoftwareModule.id!!, filePart).execute()
            } catch (e: Exception) {
                logger.error(e.message, e)
            }

            if (onError(responseUpload, client, basic, softwareModulesCreated, emptyMap())) {
                throw IOException(getErrorMessage(responseUpload))
            }
            logger.quiet(
                "File(${file.name} successfully uploaded to Update Factory server into ${newSoftwareModule.name} software module")
        }
    }

}