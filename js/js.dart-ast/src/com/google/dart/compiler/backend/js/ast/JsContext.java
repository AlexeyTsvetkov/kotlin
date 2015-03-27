// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import org.jetbrains.annotations.Nullable;

/**
 * The context in which a JsNode visitation occurs. This represents the set of
 * possible operations a JsVisitor subclass can perform on the currently visited
 * node.
 */
public abstract class JsContext<T extends JsNode> {
  public abstract boolean canInsert();

  public abstract boolean canRemove();

  public abstract <R extends T> void insertAfter(R node);

  public abstract <R extends T> void insertBefore(R node);

  public abstract boolean isLvalue();

  public abstract void removeMe();

  public abstract <R extends T> void replaceMe(R node);

  @Nullable
  public abstract T getCurrentNode();
}
