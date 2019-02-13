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

import com.kynetics.updatefactory.plugin.ufapkdelivery.Plugin.Companion.EXTENSION_NAME
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.Client
import com.kynetics.updatefactory.plugin.ufapkdelivery.api.WrapperList
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtension
import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UfApkDeliverPlugExtensionMerge
import okhttp3.Credentials
import org.gradle.api.tasks.TaskAction
import retrofit2.Response

/**
 * @author Daniele Sergio
 */

abstract class UFGetTypesTask : AbstractTask() {


    @TaskAction
    @Suppress("unused")
    fun action() {
        val defaultExtension = UFApkDeliveryPluginExtension()
        val extension = UfApkDeliverPlugExtensionMerge(getExtension<UFApkDeliveryPluginExtension>(project, EXTENSION_NAME)!!, defaultExtension)
        val client = Client(extension.buildUrl())
        val basic = Credentials.basic("${extension.tenant}\\${extension.username}", extension.password)

        try {
            val response = getTypeArray(client, basic)
            if (!onError(response, client, basic, emptyMap(), emptyMap())) {
                val array = response.body()?.content ?: emptyArray()
                if (array.isEmpty()) {
                    logger.quiet("No element found")
                } else {
                    array.forEach { ele ->
                        logger.quiet("${ele.key} -> ${ele.name}: ${ele.description}")
                    }
                }
            }
        } catch (e:Exception){
            logger.error("Communication error", e)
        }
    }

    protected abstract fun getTypeArray(client: Client, auth:String) : Response<WrapperList>
}

open class UFGetDistributionTypes: UFGetTypesTask(){
    override fun getTypeArray(client: Client, auth: String): Response<WrapperList> {
        return client.getDistributionType(auth).execute()
    }
}

open class UFGetSoftwareModuleTypes: UFGetTypesTask(){
    override fun getTypeArray(client: Client, auth: String): Response<WrapperList> {
        return client.getSoftwareModuleType(auth).execute()
    }
}