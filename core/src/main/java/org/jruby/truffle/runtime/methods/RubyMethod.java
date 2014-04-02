/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * Any kind of Ruby method - so normal methods in classes and modules, but also blocks, procs,
 * lambdas and native methods written in Java.
 */
public class RubyMethod {

    private final SourceSection sourceSection;
    private final RubyModule declaringModule;
    private final UniqueMethodIdentifier uniqueIdentifier;
    private final String name;
    private final Visibility visibility;
    private final boolean undefined;

    private final boolean appendCallNode;

    private final CallTarget callTarget;
    private final MaterializedFrame declarationFrame;
    public final boolean alwaysInline;

    public RubyMethod(SourceSection sourceSection, RubyModule declaringModule, UniqueMethodIdentifier uniqueIdentifier, String name, Visibility visibility, boolean undefined,
                    boolean appendCallNode, CallTarget callTarget, MaterializedFrame declarationFrame, boolean alwaysInline) {
        this.sourceSection = sourceSection;
        this.declaringModule = declaringModule;
        this.uniqueIdentifier = uniqueIdentifier;
        this.name = name;
        this.visibility = visibility;
        this.undefined = undefined;
        this.appendCallNode = appendCallNode;
        this.callTarget = callTarget;
        this.declarationFrame = declarationFrame;
        this.alwaysInline = alwaysInline;
    }

    public Object call(PackedFrame caller, Object self, RubyProc block, Object... args) {
        assert RubyContext.shouldObjectBeVisible(self);
        assert RubyContext.shouldObjectsBeVisible(args);

        final Object result = callX(caller, self, block, args);

        assert RubyContext.shouldObjectBeVisible(result);

        return result;
    }

    private Object callX(PackedFrame caller, Object self, RubyProc block, Object... args) {
        assert RubyContext.shouldObjectBeVisible(self);
        assert RubyContext.shouldObjectsBeVisible(args);

        RubyArguments arguments = new RubyArguments(declarationFrame, self, block, args);

        final Object result = callTarget.call(caller, arguments);

        assert RubyContext.shouldObjectBeVisible(result);

        return result;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public UniqueMethodIdentifier getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public RubyModule getDeclaringModule() { return declaringModule; }

    public String getName() {
        return name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isUndefined() {
        return undefined;
    }

    public boolean shouldAppendCallNode() {
        return appendCallNode;
    }

    public RubyMethod withNewName(String newName) {
        if (newName.equals(name)) {
            return this;
        }

        return new RubyMethod(sourceSection, declaringModule, uniqueIdentifier, newName, visibility, undefined, appendCallNode, callTarget, declarationFrame, alwaysInline);
    }

    public RubyMethod withNewVisibility(Visibility newVisibility) {
        if (newVisibility == visibility) {
            return this;
        }

        return new RubyMethod(sourceSection, declaringModule, uniqueIdentifier, name, newVisibility, undefined, appendCallNode, callTarget, declarationFrame, alwaysInline);
    }

    public RubyMethod withDeclaringModule(RubyModule newDeclaringModule) {
        if (newDeclaringModule == declaringModule) {
            return this;
        }

        return new RubyMethod(sourceSection, newDeclaringModule, uniqueIdentifier, name, visibility, undefined, appendCallNode, callTarget, declarationFrame, alwaysInline);
    }

    public RubyMethod undefined() {
        if (undefined) {
            return this;
        }

        return new RubyMethod(sourceSection, declaringModule, uniqueIdentifier, name, visibility, true, appendCallNode, callTarget, declarationFrame, alwaysInline);
    }

    public boolean isVisibleTo(RubyBasicObject caller, RubyBasicObject receiver) {
        if (caller == receiver.getRubyClass()){
            return true;
        }

        if (caller == receiver){
            return true;
        }
        return isVisibleTo(caller);
    }

    public boolean isVisibleTo(RubyBasicObject caller) {
        if (caller instanceof RubyModule) {
            if (isVisibleTo((RubyModule) caller)) {
                return true;
            }
        }

        if (isVisibleTo(caller.getRubyClass())) {
            return true;
        }

        if (isVisibleTo(caller.getSingletonClass())) {
            return true;
        }

        return false;
    }

    private boolean isVisibleTo(RubyModule module) {
        switch (visibility) {
            case PUBLIC:
                return true;

            case PROTECTED:
                if (module == declaringModule) {
                    return true;
                }

                if (module.getSingletonClass() == declaringModule) {
                    return true;
                }

                if (module.getParentModule() != null && isVisibleTo(module.getParentModule())) {
                    return true;
                }

                return false;

            case PRIVATE:
                if (module == declaringModule) {
                    return true;
                }

                if (module.getSingletonClass() == declaringModule) {
                    return true;
                }

                if (module.getParentModule() != null && isVisibleTo(module.getParentModule())) {
                    return true;
                }

                return false;

            default:
                return false;
        }
    }

    public MaterializedFrame getDeclarationFrame() {
        return declarationFrame;
    }

    public CallTarget getCallTarget(){
        return callTarget;
    }

    public boolean isAlwaysInlined() {
        return alwaysInline;
    }

}
