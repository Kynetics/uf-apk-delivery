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

package com.kynetics.updatefactory.plugin.ufapkdelivery.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * @author Daniele Sergio
 */

interface IdentifiableObject{
    val id:Long?
}
typealias ObjectDeletion = (String, Long) -> Call<Void>

data class Error(val exceptionClass:String = "", val errorCode:String = "", val message:String = "Error")
data class SoftwareModule(val vendor:String, val name:String, val description:String, val type: String, val version:String, override val id:Long? = null) : IdentifiableObject
data class Distribution(val name:String, val description:String, val type: String, val version:String, val modules:Set<Id>, override val id:Long? = null, val requiredMigrationStep:Boolean = false) : IdentifiableObject
data class Id(val id: Long)
data class ObjectType(val name:String, val description:String, val key:String)
data class WrapperList(val content:Array<ObjectType>)

interface ManagementApi {
    companion object {
        const val BASE_V1_REQUEST_MAPPING = "/rest/v1"
    }

    @Headers("Accept: application/hal+json")
    @GET("$BASE_V1_REQUEST_MAPPING/distributionsettypes")
    fun getDistributionType(@Header("Authorization") auth: String): Call<WrapperList>

    @Headers("Accept: application/hal+json")
    @GET("$BASE_V1_REQUEST_MAPPING/softwaremoduletypes")
    fun getSoftwareModuleType(@Header("Authorization") auth: String): Call<WrapperList>

    @Headers("Accept: application/hal+json")
    @POST("$BASE_V1_REQUEST_MAPPING/softwaremodules")
    fun createSoftwareModule(@Header("Authorization") auth: String,
                             @Body softwareModule: Array<SoftwareModule>): Call<Array<SoftwareModule>>

    @Headers("Accept: application/hal+json")
    @DELETE("$BASE_V1_REQUEST_MAPPING/softwaremodules/{softwareModuleId}")
    fun deleteSoftwareModule(@Header("Authorization") auth: String,
                             @Path("softwareModuleId") softwareModuleId: Long): Call<Void>

    @Multipart
    @POST("$BASE_V1_REQUEST_MAPPING/softwaremodules/{softwareModuleId}/artifacts")
    fun uploadFile(@Header("Authorization") auth: String,
                   @Path("softwareModuleId") softwareModuleId: Long,
                   @Part filePart: MultipartBody.Part): Call<Any>

    @Headers("Accept: application/hal+json")
    @POST("$BASE_V1_REQUEST_MAPPING/distributionsets")
    fun createDistribution(@Header("Authorization") auth: String,
                             @Body distribution: List<Distribution>): Call<Array<Distribution>>

    @Headers("Accept: application/hal+json")
    @DELETE("$BASE_V1_REQUEST_MAPPING/distributionsets/{distributionId}")
    fun deleteDistribution(@Header("Authorization") auth: String,
                             @Path("distributionId") distributionId: Long): Call<Void>
}


class Client(url: String) : ManagementApi {
    private val delegate: ManagementApi = Retrofit.Builder().baseUrl(url)
            .client( OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ManagementApi::class.java)

    override fun createSoftwareModule(auth: String, softwareModule: Array<SoftwareModule>): Call<Array<SoftwareModule>> {
        return delegate.createSoftwareModule(auth, softwareModule)
    }

    override fun uploadFile(auth: String, softwareModuleId: Long, filePart: MultipartBody.Part): Call<Any> {
        return delegate.uploadFile(auth, softwareModuleId, filePart)
    }

    override fun createDistribution(auth: String, distribution: List<Distribution>): Call<Array<Distribution>> {
        return delegate.createDistribution(auth, distribution)
    }

    override fun deleteSoftwareModule(auth: String, softwareModuleId: Long): Call<Void> {
        return delegate.deleteSoftwareModule(auth, softwareModuleId)
    }

    override fun deleteDistribution(auth: String, distributionId: Long): Call<Void> {
        return delegate.deleteDistribution(auth,distributionId)
    }

    override fun getDistributionType(auth: String): Call<WrapperList> {
        return delegate.getDistributionType(auth)
    }

    override fun getSoftwareModuleType(auth: String): Call<WrapperList> {
        return delegate.getSoftwareModuleType(auth)
    }
}