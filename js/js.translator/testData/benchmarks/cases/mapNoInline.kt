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

import java.util.ArrayList

native fun Array<Int>.push(x: Int): Unit = noImpl

noinline fun Array<Int>.mapTo(destination: Array<Int>, transform: (Int) -> Int): Array<Int> {
    for (item in this)
        destination.push(transform(item))

    return destination
}

noinline fun Array<Int>.map(transform: (Int) -> Int): Array<Int> {
    return mapTo(array(), transform)
}

fun test(target: Array<Int>) {
    val multiplier = 3
    val result = target.map { it*multiplier }
}

fun box(): String {
    return "OK"
}