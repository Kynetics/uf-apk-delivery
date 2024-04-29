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
package com.kynetics.updatefactory.plugin.ufapkdelivery

import com.kynetics.updatefactory.plugin.ufapkdelivery.extension.UFApkDeliveryPluginExtension
import com.kynetics.updatefactory.plugin.ufapkdelivery.task.UFApkDeliveryTask
import com.kynetics.updatefactory.plugin.ufapkdelivery.task.UFGetDistributionTypes
import com.kynetics.updatefactory.plugin.ufapkdelivery.task.UFGetSoftwareModuleTypes
import com.kynetics.updatefactory.plugin.ufapkdelivery.task.UFShowSubprojectsTask

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Daniele Sergio
 */
class Plugin : Plugin<Project> {
    companion object {
        const val EXTENSION_NAME = "ufApkDelivery"
        const val GROUP_NAME = "update factory"
    }
    override fun apply(project: Project) {
        project.extensions.run{
            create(EXTENSION_NAME, UFApkDeliveryPluginExtension::class.java)
        }

        project.subprojects.forEach{p->
            p.extensions.run{
                create(EXTENSION_NAME, UFApkDeliveryPluginExtension::class.java)
            }

        }

        with(project.tasks) {
            create("deliveryApk", UFApkDeliveryTask::class.java) {
                it.group = GROUP_NAME
                it.description = "Delivery application to Update Factory"
            }

            create("getSoftwareModuleTypes", UFGetSoftwareModuleTypes::class.java) {
                it.group = GROUP_NAME
                it.description = "Get legal software module types"
            }

            create("getDistributionTypes", UFGetDistributionTypes::class.java) {
                it.group = GROUP_NAME
                it.description = "Get legal distribution types"
            }

            create("showSubProjects", UFShowSubprojectsTask::class.java) {
                it.group = GROUP_NAME
                it.description = "Show list of sub projects project "
            }


        }
    }

}