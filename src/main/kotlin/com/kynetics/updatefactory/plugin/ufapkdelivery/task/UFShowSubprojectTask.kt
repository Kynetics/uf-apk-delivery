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

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

/**
 * @author Daniele Sergio
 */

@CacheableTask
open class UFShowSubprojectsTask : AbstractTask() {

    @TaskAction
    @Suppress("unused")
    fun action() {
        project.subprojects.forEach{p -> logger.quiet(p.name) }
    }

}