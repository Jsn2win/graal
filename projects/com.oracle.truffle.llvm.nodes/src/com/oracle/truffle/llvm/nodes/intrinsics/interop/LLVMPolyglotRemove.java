/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.nodes.intrinsics.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMIntrinsic;
import com.oracle.truffle.llvm.runtime.LLVMPolyglotException;
import com.oracle.truffle.llvm.runtime.interop.LLVMAsForeignNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;

public final class LLVMPolyglotRemove {

    @NodeChild(type = LLVMExpressionNode.class)
    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMPolyglotRemoveMember extends LLVMIntrinsic {

        @Child private Node foreignRemove = Message.REMOVE.createNode();
        @Child private LLVMAsForeignNode asForeign = LLVMAsForeignNode.create();

        @Specialization
        protected boolean doRemove(LLVMManagedPointer value, Object id,
                        @Cached("createReadString()") LLVMReadStringNode readStr) {
            TruffleObject foreign = asForeign.execute(value);
            try {
                return ForeignAccess.sendRemove(foreignRemove, foreign, readStr.executeWithTarget(id));
            } catch (UnknownIdentifierException ex) {
                CompilerDirectives.transferToInterpreter();
                throw new LLVMPolyglotException(this, "Member '%s' does not exist.", ex.getUnknownIdentifier());
            } catch (UnsupportedMessageException ex) {
                CompilerDirectives.transferToInterpreter();
                throw new LLVMPolyglotException(this, "Can not remove member '%s' from polyglot value.", id);
            }
        }

        @Fallback
        @TruffleBoundary
        @SuppressWarnings("unused")
        public boolean error(Object value, Object id) {
            throw new LLVMPolyglotException(this, "Invalid argument to polyglot builtin.");
        }
    }

    @NodeChild(type = LLVMExpressionNode.class)
    @NodeChild(type = LLVMExpressionNode.class)
    public abstract static class LLVMPolyglotRemoveArrayElement extends LLVMIntrinsic {

        @Child private Node foreignRemove = Message.REMOVE.createNode();
        @Child private LLVMAsForeignNode asForeign = LLVMAsForeignNode.create();

        @Specialization
        protected boolean doRemove(LLVMManagedPointer value, int idx) {
            TruffleObject foreign = asForeign.execute(value);
            try {
                return ForeignAccess.sendRemove(foreignRemove, foreign, idx);
            } catch (UnknownIdentifierException ex) {
                throw new LLVMPolyglotException(this, "Index %d does not exist.", idx);
            } catch (UnsupportedMessageException ex) {
                throw new LLVMPolyglotException(this, "Can not remove index %d from polyglot value.", idx);
            }
        }

        @Fallback
        @TruffleBoundary
        @SuppressWarnings("unused")
        public boolean fallback(Object value, Object id) {
            throw new LLVMPolyglotException(this, "Invalid argument to polyglot builtin.");
        }
    }
}
