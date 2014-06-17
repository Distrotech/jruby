/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.RubyArray;

/**
 * Concatenate arrays.
 */
@NodeInfo(shortName = "array-concat")
public final class ArrayConcatNode extends RubyNode {

    @Children protected final RubyNode[] children;
    @Child protected ArrayAllocationSite arrayAllocationSite;

    public ArrayConcatNode(RubyContext context, SourceSection sourceSection, RubyNode[] children) {
        super(context, sourceSection);
        assert children.length > 1;
        this.children = children;
        arrayAllocationSite = new ArrayAllocationSite.UninitializedArrayAllocationSite(context);
    }

    @Override
    public RubyArray execute(VirtualFrame frame) {
        notDesignedForCompilation();

        int length = 0;

        for (int n = 0; n < children.length; n++) {
            final Object childObject = children[n].execute(frame);

            if (childObject instanceof RubyArray) {
                final RubyArray childArray = (RubyArray) childObject;
                length += childArray.getSize();
            } else {
                length++;
            }
        }

        Object store = arrayAllocationSite.start(length);
        int index = 0;

        for (int n = 0; n < children.length; n++) {
            final Object childObject = children[n].execute(frame);

            if (childObject instanceof RubyArray) {
                final RubyArray childArray = (RubyArray) childObject;
                store = arrayAllocationSite.set(store, index, childArray.getStore(), childArray.getSize());
                index += childArray.getSize();
            } else {
                store = arrayAllocationSite.set(store, index, childObject);
                index++;
            }
        }

        return arrayAllocationSite.finish(store, length);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        notDesignedForCompilation();

        for (int n = 0; n < children.length; n++) {
            children[n].executeVoid(frame);
        }
    }

}
