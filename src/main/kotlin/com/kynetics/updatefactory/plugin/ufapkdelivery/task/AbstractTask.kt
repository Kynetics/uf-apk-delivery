/*
 *
 *  Copyright (c) 2024 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 */
package com.kynetics.updatefactory.plugin.ufapkdelivery.task

import com.kynetics.updatefactory.plugin.ufapkdelivery.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import retrofit2.Response
import java.io.File

/**
 * @author Daniele Sergio
 */
abstract class AbstractTask: DefaultTask() {
    protected inline fun<reified T> getExtension(project: Project, extensionName:String): T? {
        return project.extensions.run {
            findByName(extensionName) as T?
        }
    }

    protected fun clearIdentifiableList(softwareModulesCreated: Map<Id, *>, deletionFunction: ObjectDeletion, basic: String): Map<Id, SoftwareModule> {
        softwareModulesCreated.forEach { _, identifiableObject ->
            deleteIdentifiableObject(deletionFunction, basic, identifiableObject as IdentifiableObject)
        }
        return emptyMap()
    }

    protected fun getDirectoryOutputFiles(project: Project, buildType: String, productFlavor:String):List<File>{
        val dir = File(project.buildDir, "outputs${File.separator}apk${File.separator}$productFlavor${File.separator}$buildType")
        if(!dir.exists()){
            logger.error("Apk directory not found (${dir.absolutePath})")
            return emptyList()
        }
        return dir.listFiles().filter{file -> file.name.endsWith("apk")}
    }

    private fun printError(response: Response<*>?){
        val errorMessage = getErrorMessage(response)
        if(errorMessage != null){
            logger.error("Error ${response?.code()}, ${response?.message()}")
            logger.error(errorMessage)
        } else {
            logger.error("No response from the server!")
        }
    }

    protected fun getErrorMessage(response: Response<*>?): String? {
        if (response == null) {
            return null
        }
        val errorBody = response.errorBody()
        return if (!response.isSuccessful && errorBody != null) {
            UFApkDeliveryTask.gson.fromJson(errorBody.string(), Error::class.java)?.message
        } else null
    }

    protected fun onError(
            response: Response<*>?,
            client: ManagementApi,
            basic: String,
            newSoftwareModulesCreated: Map<Id, SoftwareModule>,
            newDistributionsCreated: Map<Id, Distribution>) :Boolean {
        if (response == null || !response.isSuccessful) {
            printError(response)
            clearIdentifiableList(newDistributionsCreated, client::deleteDistribution, basic)
            clearIdentifiableList(newSoftwareModulesCreated, client::deleteSoftwareModule, basic)
            return true
        }
        return false
    }

    private fun deleteIdentifiableObject(delete: ObjectDeletion, basic: String, objectToDelete: IdentifiableObject) {
        val deleteResponse = delete.invoke(basic, objectToDelete.id!!).execute()
        if (deleteResponse.isSuccessful) {
            logger.quiet("$objectToDelete deleted")
        } else {
            logger.warn("Fail to delete $objectToDelete")
        }
    }
}