/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package foo

inline fun Array<Int>.forEach(operation: (Int) -> Unit): Unit {
    for (element in this) operation(element)
}

fun test(a: Array<Int>) {
    var sum = 0
    a.forEach { sum += it }
}

fun box(): String {
    test(array(1,2,3))

    return "OK"
}