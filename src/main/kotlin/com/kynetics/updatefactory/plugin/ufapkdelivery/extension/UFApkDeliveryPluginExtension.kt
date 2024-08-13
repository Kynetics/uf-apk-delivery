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
package com.kynetics.updatefactory.plugin.ufapkdelivery.extension

/**
 * @author Daniele Sergio
 */

interface UFApkDeliveryPluginExtensionInterface{
    var tenant: String
    var username: String
    var password: String
    var softwareModuleType: String
    var distributionType: String
    var vendor: String
    var buildType: String
    var productFlavor: String
    var deliveryType : DeliveryType
    var ignoreProjects : MutableList<String>
    var version : String
    var thirdLevelDomain: String
    var softwareModuleName: String

    enum class DeliveryType{
        MULTIPLE_DISTRIBUTION, SINGLE_SOFTWARE_MODULE, MULTIPLE_SOFTWARE_MODULE
    }

    fun buildUrl():String
}

open class UFApkDeliveryPluginExtension : UFApkDeliveryPluginExtensionInterface {
    companion object {
        const val protocol = "https"
        const val url = ".updatefactory.io"
    }

    override var tenant: String = ""
    override var username: String = ""
    override var password: String = ""
    override var softwareModuleType: String = ""
    override var distributionType: String = ""
    override var vendor: String = ""
    override var buildType: String = ""
    override var productFlavor: String = ""
    override var deliveryType : UFApkDeliveryPluginExtensionInterface.DeliveryType = UFApkDeliveryPluginExtensionInterface.DeliveryType.MULTIPLE_SOFTWARE_MODULE
    override var ignoreProjects : MutableList<String> = mutableListOf()
    override var version : String = ""
    override var thirdLevelDomain : String = "mgmt.business"
    override var softwareModuleName : String = ""

    override fun buildUrl(): String {
        return "$protocol://$thirdLevelDomain$url"
    }
}

class UfApkDeliverPlugExtensionMerge(private val delegate: UFApkDeliveryPluginExtensionInterface, fallback: UFApkDeliveryPluginExtensionInterface) : UFApkDeliveryPluginExtensionInterface by delegate{
    companion object {
        fun getOrDefault(value: String, fallback: String) :String{
            return if(value.isEmpty()) fallback else value
        }
    }

    init{
        softwareModuleType = getOrDefault(delegate.softwareModuleType, fallback.softwareModuleType)
        distributionType = getOrDefault(delegate.distributionType, fallback.distributionType)
        buildType = getOrDefault(delegate.buildType, fallback.buildType)
        productFlavor = getOrDefault(delegate.productFlavor, fallback.productFlavor)
        softwareModuleName = getOrDefault(delegate.softwareModuleName, fallback.softwareModuleName)
    }
}