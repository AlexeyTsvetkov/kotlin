/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.utils.serializer

import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializerExtension
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.js.*
import org.jetbrains.kotlin.serialization.NameSerializationUtil
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.utils.LibraryUtils

import java.io.ByteArrayOutputStream
import java.io.File

public class KotlinJavaScriptSerializer() {

    public fun serialize(moduleName: String, moduleDescriptor: ModuleDescriptor, metaFile: File) {
        val contentMap = hashMapOf<String, ByteArray>()
        serialize(moduleDescriptor) {
            (fileName, stream) -> contentMap[fileName] = stream.toByteArray()
        }

        val content = KotlinJavascriptSerializationUtil.contentMapToByteArray(contentMap)
        LibraryUtils.writeMetadata(moduleName, content, metaFile)
    }

    public fun serialize(moduleDescriptor: ModuleDescriptor, writeFun: (String, ByteArrayOutputStream) -> Unit) {
        getPackagesFqNames(moduleDescriptor).forEach {
            fqName -> serializePackage(moduleDescriptor, fqName, writeFun)
        }
    }

    fun serializePackage(module: ModuleDescriptor, fqName: FqName, writeFun: (String, ByteArrayOutputStream) -> Unit) {
        val packageView = module.getPackage(fqName) ?: error("No package resolved in $module")

        // TODO: perform some kind of validation? At the moment not possible because DescriptorValidator is in compiler-tests
        // DescriptorValidator.validate(packageView)

        val skip: (DeclarationDescriptor) -> Boolean = { DescriptorUtils.getContainingModule(it) != module} //{ isBuiltin(it) }

        val serializer = DescriptorSerializer.createTopLevel(BuiltInsSerializerExtension)

        val classifierDescriptors = DescriptorSerializer.sort(packageView.getMemberScope().getDescriptors(DescriptorKindFilter.CLASSIFIERS))

        ClassSerializationUtil.serializeClasses(classifierDescriptors, serializer, object : ClassSerializationUtil.Sink {
            override fun writeClass(classDescriptor: ClassDescriptor, classProto: ProtoBuf.Class) {
                val stream = ByteArrayOutputStream()
                classProto.writeTo(stream)
                writeFun(getFileName(classDescriptor), stream)
            }
        }, skip)

        val packageStream = ByteArrayOutputStream()
        val fragments = module.getPackageFragmentProvider().getPackageFragments(fqName)
        val packageProto = serializer.packageProto(fragments, skip).build() ?: error("Package fragments not serialized: $fragments")
        packageProto.writeTo(packageStream)
        writeFun(BuiltInsSerializationUtil.getPackageFilePath(fqName), packageStream)

        val nameStream = ByteArrayOutputStream()
        NameSerializationUtil.serializeStringTable(nameStream, serializer.getStringTable())
        writeFun(BuiltInsSerializationUtil.getStringTableFilePath(fqName), nameStream)
    }

    fun getFileName(classDescriptor: ClassDescriptor): String {
        return BuiltInsSerializationUtil.getClassMetadataPath(classDescriptor.classId)
    }

    private fun getPackagesFqNames(moduleDescriptor: ModuleDescriptor): Set<FqName> {
        val rootPackageView = moduleDescriptor.getPackage(FqName.ROOT)!!
        return getSubPackagesFqNames(rootPackageView)
    }

    private fun getSubPackagesFqNames(packageView: PackageViewDescriptor): Set<FqName> {
        val result = hashSetOf<FqName>()

        fun helper(packageView: PackageViewDescriptor) {
            val fqName = packageView.getFqName()
            if (!fqName.isRoot()) {
                result.add(fqName)
            }
            packageView.getMemberScope().getDescriptors(DescriptorKindFilter.PACKAGES, JetScope.ALL_NAME_FILTER).forEach { if (it is PackageViewDescriptor) helper(it) }
        }

        helper(packageView)
        return result
    }
}