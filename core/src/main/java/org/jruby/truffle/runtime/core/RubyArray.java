/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.Options;

import java.util.Arrays;

/**
 * Implements the Ruby {@code Array} class.
 */
public final class RubyArray extends RubyObject {

    public static class RubyArrayClass extends RubyClass {

        public RubyArrayClass(RubyClass objectClass) {
            super(null, objectClass, "Array");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyArray(this, null, 0);
        }

    }

    private Object store;
    private int size;

    public static RubyArray slowNewArray(RubyClass arrayClass) {
        RubyNode.notDesignedForCompilation();
        return new RubyArray(arrayClass);
    }

    public static RubyArray slowNewArray(RubyClass arrayClass, Object store, int size) {
        RubyNode.notDesignedForCompilation();
        return new RubyArray(arrayClass, store, size);
    }

    public static RubyArray unsafeNewArray(RubyClass arrayClass) {
        return new RubyArray(arrayClass);
    }

    public static RubyArray unsafeNewArray(RubyClass arrayClass, Object store, int size) {
        return new RubyArray(arrayClass, store, size);
    }

    private RubyArray(RubyClass arrayClass) {
        this(arrayClass, null, 0);
    }

    private RubyArray(RubyClass arrayClass, Object store, int size) {
        super(arrayClass);

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;

        // TODO: assert that an object array doesn't contain all primitives - performance warning?

        this.store = store;
        this.size = size;
    }

    public static RubyArray slowFromObject(RubyClass arrayClass, Object object) {
        RubyNode.notDesignedForCompilation();

        final Object store;

        if (object instanceof Integer && Options.TRUFFLE_ARRAYS_INT.load()) {
            store = new int[]{(int) object};
        } else if (object instanceof RubyFixnum.IntegerFixnum && Options.TRUFFLE_ARRAYS_INT.load()) {
            store = new int[]{((RubyFixnum.IntegerFixnum) object).getValue()};
        } else if (object instanceof Long && Options.TRUFFLE_ARRAYS_LONG.load()) {
            store = new long[]{(long) object};
        } else if (object instanceof RubyFixnum.LongFixnum && Options.TRUFFLE_ARRAYS_LONG.load()) {
            store = new long[]{((RubyFixnum.LongFixnum) object).getValue()};
        } else if (object instanceof Double && Options.TRUFFLE_ARRAYS_DOUBLE.load()) {
            store = new double[]{(double) object};
        } else if (object instanceof RubyFloat && Options.TRUFFLE_ARRAYS_DOUBLE.load()) {
            store = new double[]{((RubyFloat) object).getValue()};
        } else {
            store = new Object[]{object};
        }

        return new RubyArray(arrayClass, store, 1);
    }

    public static RubyArray slowFromObjects(RubyClass arrayClass, Object... objects) {
        RubyNode.notDesignedForCompilation();

        if (objects.length == 0) {
            return new RubyArray(arrayClass);
        }

        if (objects.length == 1) {
            return slowFromObject(arrayClass, objects[0]);
        }

        boolean canUseInteger = Options.TRUFFLE_ARRAYS_INT.load();
        boolean canUseLong = Options.TRUFFLE_ARRAYS_LONG.load();
        boolean canUseDouble = Options.TRUFFLE_ARRAYS_DOUBLE.load();

        for (Object object : objects) {
            if (object instanceof Integer) {
                canUseDouble = false;
            } else if (object instanceof RubyFixnum.IntegerFixnum) {
                canUseDouble = false;
            } else if (object instanceof Long) {
                canUseInteger = canUseInteger && RubyFixnum.fitsIntoInteger((long) object);
                canUseDouble = false;
            } else if (object instanceof RubyFixnum.LongFixnum) {
                canUseInteger = canUseInteger && RubyFixnum.fitsIntoInteger(((RubyFixnum.LongFixnum) object).getValue());
                canUseDouble = false;
            } else if (object instanceof Double) {
                canUseInteger = false;
                canUseLong = false;
            } else if (object instanceof RubyFloat) {
                canUseInteger = false;
                canUseLong = false;
            } else {
                canUseInteger = false;
                canUseLong = false;
                canUseDouble = false;
            }
        }

        if (canUseInteger) {
            final int[] store = new int[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = RubyFixnum.toInt(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else if (canUseLong) {
            final long[] store = new long[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = RubyFixnum.toLong(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else if (canUseDouble) {
            final double[] store = new double[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = RubyFloat.toDouble(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else {
            return new RubyArray(arrayClass, objects, objects.length);
        }
    }

    public Object[] slowToArray() {
        RubyNode.notDesignedForCompilation();

        return Arrays.copyOf(ArrayUtils.box(store), size);
    }

    public Object slowShift() {
        CompilerAsserts.neverPartOfCompilation();

        if (size == 0) {
            return NilPlaceholder.INSTANCE;
        } else {
            store = ArrayUtils.box(store);
            final Object value = ((Object[]) store)[0];
            System.arraycopy(store, 1, store, 0, size - 1);
            size--;
            return value;
        }
    }

    public void slowUnshift(Object... values) {
        RubyNode.notDesignedForCompilation();

        final Object[] newStore = new Object[size + values.length];
        System.arraycopy(values, 0, newStore, 0, values.length);
        ArrayUtils.copy(store, newStore, values.length, size);
        setStore(newStore, newStore.length);
    }

    public void slowPush(Object value) {
        RubyNode.notDesignedForCompilation();

        store = Arrays.copyOf(ArrayUtils.box(store), size + 1);
        ((Object[]) store)[size] = value;
        size++;
    }

    public int normaliseIndex(int index) {
        return normaliseIndex(size, index);
    }

    public int normaliseExclusiveIndex(int index) {
        return normaliseExclusiveIndex(size, index);
    }

    public static int normaliseIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public static int normaliseExclusiveIndex(int length, int exclusiveIndex) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, exclusiveIndex < 0)) {
            return length + exclusiveIndex + 1;
        } else {
            return exclusiveIndex;
        }
    }

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int size) {
        this.store = store;
        this.size = size;

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;
        assert !(store instanceof int[]) || Options.TRUFFLE_ARRAYS_INT.load();
        assert !(store instanceof long[]) || Options.TRUFFLE_ARRAYS_LONG.load();
        assert !(store instanceof double[]) || Options.TRUFFLE_ARRAYS_DOUBLE.load();

        // TODO: assert that an object array doesn't contain all primitives - performance warning?
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;
    }


}
