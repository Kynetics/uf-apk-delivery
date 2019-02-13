# Apk delivery plugin
This plugin automates the upload of applications to Update Factory server.

# Plugin import
To use the plugin:

1. publish it to your maven local repository (see *Publish to maven local* section);
2. import it into your Android project (see *Import to Android project* section).

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
    id 'com.kynetics.updatefactory.plugin.ufapkdelivery' version("1.0.0")
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
    thirdLevelDomain = "personal"

}
```
Replace the `ufApkDelivery` data with your applications and Update Factory access informations.  

It is possible to specifiy different `buildType`, `productFlavor`, `softwareModuleType` or `distributionType` for different modules of the same project. To do that, add the following lines to the module specific `build.gradle`:
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

The following tasks are available.

## deliveryApk
Upload the project in the Update Factory server.

The error message `The given entity already exists in database` occurs even if the entity isn't visible by the ui. This can happen when an entity is deleted from UF, as UF sometimes retains metadata for historical purpose.

## getDistributionTypes
Show all the distribution types supported by the tenant.

## getSoftwareModuleTypes
Show all the software module types supported by the tenant.

## showSubProjects
Show a list of all sub projects. To ignore a module adds its name to ignoreProjects field.

# Plugin Configurations
The extension values of `Apk delivery plugin ` is contained in an object called `ufApkDelivery`.

## tenant (mandatory)
The Update Factory tenant.

## username (mandatory)
The username to login into Update Factory tenant.

## password (mandatory)
The password to login into Update Factory tenant.

## thirdLevelDomain (optional)
Third level domain value depends by your subscription type:

- `personal` (default value) : if the name of your subscription is one between `Free` or `Development`
- `business` : if the name of your subscription is one of `Proof of Concept`, `Expansion` or `Commercial`

## softwareModuleType (optional)
The type of software module that will be create.

The default value is `application`, typically this value should not be changed.

## distributionType (optional)
The type of distribution that will be create.

The default value is `app`, typically this value should not be changed.

## vendor (optional)
The vendor of software module.

## buildType (optional)
The build type of the project to upload.
Default value is `release`.

## productFlavor (optional)
The productFlavor of software module to upload (if exist).

## deliveryType (optional)
Exist three type of delivery:

1. *SINGLE_SOFTWARE_MODULE*: it is created a software module in Update Factory server that contains all the apk (one for each project module);
2. *MULTIPLE_SOFTWARE_MODULE* (default): every project modules is mapped in an Update Factory software module, all software modules is added to a distribution;
3. *MULTIPLE_DISTRIBUTION*(: every project modules is mapped in an Update Factory distribution (that contains a software module).

### version (mandatory)
The distribution version, this field is mandatory if the build type is one between SINGLE_SOFTWARE_MODULE and MULTIPLE_SOFTWARE_MODULE otherwise is ignored.

## ignoreProjects (optional)
List of all modules that mustn't be upload to Update Factory.

# License

Copyright (c) 2019, Kynetics LLC. 

Released under the EPLv2.0 License.