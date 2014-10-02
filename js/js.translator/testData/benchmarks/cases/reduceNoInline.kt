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


noinline fun Array<Int>.reduce(operation: (Int, Int) -> Int): Int {
    val it = this.iterator()
    if (!it.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced")
    var accumulator = it.next()
    while (it.hasNext()) {
        accumulator = operation(accumulator, it.next())
    }
    return accumulator
}

fun test(target: Array<Int>) {
    val sum = target.reduce { (x, y) -> x + y }
}

fun box(): String {
    return "OK"
}