# Apk delivery plugin
This plugin automates the upload of applications to Update Factory server.
[uf-apk-delivery-example](https://github.com/Kynetics/uf-apk-delivery-example) is an example project that uses this plugin.

# Table of contents
<!--ts-->
   * [Apk delivery plugin](#apk-delivery-plugin)
   * [Table of contents](#table-of-contents)
   * [Plugin import](#plugin-import)
        * [Publish to maven local](#publish-to-maven-local)
        * [Import to Android project](#import-to-android-project)
   * [Tasks](#tasks)
   * [Plugin Configurations](#plugin-configurations)
   * [Common Errors](#common-errors)

<!--te-->

# Plugin import
To use the plugin:

1. [publish it to your maven local repository](#publish-to-maven-local)
2. [import it into your Android project](#import-to-android-project)

## Publish to maven local
Push `Apk delivery plugin` into your maven local repository with the following command:

```
./gradlew clean build publishToMavenLocal
```

## Import to Android project
Update your settings.gradle and build.gradle to use `Apk delivery plugin` into your android project:

### settings.gradle
Add these lines to the top of your settings.gradle file:

```
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        jcenter()
        google()
    }
}
```

### build.gradle (root)

Import `Apk delivery plugin` inside your android project:
```
buildscript {

    ...

    repositories {

        ...

        mavenLocal()
    }

    ...
}

plugins{
    id 'com.kynetics.updatefactory.plugin.ufapkdelivery' version("1.0.1")
}

...

ufApkDelivery {
    tenant = 'tenant'
    username = 'username'
    password = 'password'
    vendor = 'kynetics'
    version = '3'
    buildType = "release"
    productFlavor = "full"
    ignoreProjects = ["project-name"]
    deliveryType = "SINGLE_SOFTWARE_MODULE"
    thirdLevelDomain = "mgmt.business"
    softwareModuleName = "software-module-name"
}
```
Replace the `ufApkDelivery` data with your applications and Update Factory access information.  

It is possible to specify different `buildType`, `productFlavor`, `softwareModuleType` or `distributionType` for different modules of the same project. To do that, add the following lines to the module specific `build.gradle`:
```
...

ufApkDelivery {
	softwareModuleType = "application"
    distributionType = "app"
    buildType = "release"
    productFlavor = "full"

}
```
These values, if present, are used instead on the generic values of the root `build.gradle`.

# Tasks
When the plugin is correctly imported, the group `update factory` appears under the main project (root) of gradle tasks window.

The following tasks are available:
- deliveryApk, uploads the project in the Update Factory server;
- getDistributionTypes, shows all the distribution types supported by the tenant;
- getSoftwareModuleTypes, shows all the software module types supported by the tenant;
- showSubProjects, show a list of all sub projects. To ignore a module adds its name to ignoreProjects field.

# Plugin Configurations
The extension values of `Apk delivery plugin ` is contained in an object called `ufApkDelivery`.

| Field              | Description                                                                                                                                                                                  |        Mandatory         |                                                                                     Default                                                                                     | Allowed value                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| tenant             | The Update Factory tenant                                                                                                                                                                    |    :heavy_check_mark:    |                                                                                        -                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| username           | The username to login into Update Factory tenant                                                                                                                                             |    :heavy_check_mark:    |                                                                                        -                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| password           | The password to login into Update Factory tenant.                                                                                                                                            |    :heavy_check_mark:    |                                                                                        -                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| thirdLevelDomain   | Third level domain value depends on your subscription type                                                                                                                                   | :heavy_multiplication_x: |                                                                                 `mgmt.business`                                                                                 | <ul><li>`mgmt.personal`: if the name of your subscription is `Free` or `Development`</li><li>`mgmt.business`: if the name of your subscription `Proof of Concept`, `Expansion` or `Commercial`</li><li>See [UF Platform URLs](https://docs.updatefactory.io/platform/platform-overview/#platform-urls) for more details.</li></ul>                                                                                                                                 |
| softwareModuleType | The type of software module that will be created                                                                                                                                             | :heavy_multiplication_x: |                                                                                  `application`                                                                                  | Use `getSoftwareModuleTypes` task to view allowed values                                                                                                                                                                                                                                                                                                                                                                                                           |
| distributionType   | The type of distribution that will be created                                                                                                                                                | :heavy_multiplication_x: |                                                                                      `app`                                                                                      | Use `getDistributionTypes` task to view allowed values                                                                                                                                                                                                                                                                                                                                                                                                             |
| vendor             | The vendor of software module                                                                                                                                                                | :heavy_multiplication_x: |                                                                                       ""                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| buildType          | The build type of the project to upload                                                                                                                                                      | :heavy_multiplication_x: |                                                                                    `release`                                                                                    | All project build types                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| productFlavor      | The build type of the project to upload                                                                                                                                                      | :heavy_multiplication_x: |                                                                                       ""                                                                                        | The productFlavor of software module to upload (if exist)                                                                                                                                                                                                                                                                                                                                                                                                          |
| deliveryType       | Define what resources are created in the Update Factory                                                                                                                                      | :heavy_multiplication_x: |                                                                           `MULTIPLE_SOFTWARE_MODULE`                                                                            | <ul> <li>`SINGLE_SOFTWARE_MODULE`, it creates a software module in Update Factory server that contains all the apk (one for each project module)</li><li>`MULTIPLE_SOFTWARE_MODULE`, every project modules is mapped in an Update Factory software module, all software modules is added to a single distribution;</li> <li>`MULTIPLE_DISTRIBUTION`,every project modules is mapped in an Update Factory distribution (that contains a software module).</li></ul> |
| version            | The distribution version. This option is ignored if the delivery type is `MULTIPLE_DISTRIBUTION`                                                                                             |    :heavy_check_mark:    |                                                                                        -                                                                                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ignoreProjects     | List of all modules that mustn't be upload to Update Factory.                                                                                                                                | :heavy_multiplication_x: |                                                                                   Empty list                                                                                    |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| softwareModuleName | The name of the software module and the distribution that will be created. If the delivery type is other than `SINGLE_SOFTWARE_MODULE`, the module name will added as a suffix to this value | :heavy_multiplication_x: | <ul> <li>The project name when choosing `SINGLE_SOFTWARE_MODULE` delivery type.</li><li>The project name + module name (As suffix) when choosing other delivery types</li></ul> |                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |

# Common Errors
These are the most common errors message.

## The given entity already exists in database.
This error happens when the plugin tries to create some software modules or distributions that already exist.

It can occur even if the entity isn't visible by the ui. This can happen when an entity is deleted from Update Factory, as Update Factory sometimes retains metadata.
### Solution
Change the version of your gradle module or the version field.

## Distribution set type does not contain the given module, i.e. is incompatible.
The distribution type doesn't support the software module type.
### Solution
Change your distribution type or your software module type.

## DistributionSetType with given identifier {distributionType} does not exist.
The distribution type doesn't exist. 
### Solution
Set the distributionType field with one value returned by the `getSoftwareModuleTypes` task.

## SoftwareModuleType with given identifier {softwareModuleType} does not exist.
The software module type doesn't exist. 
### Solution
Set the softwareModuleType field with one value returned by the `getDistributionTypes` task.

## Unauthorized
Wrong credentials
### Solution
Check the `username` and `password` fields.

# License

Copyright (c) 2024, Kynetics LLC. 

Released under the EPLv2.0 License.
