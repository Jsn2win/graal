/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.truffle.runtime;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.graalvm.compiler.truffle.common.CompilableTruffleAST;
import org.graalvm.compiler.truffle.common.TruffleCompilerListener;
import org.graalvm.compiler.truffle.common.TruffleInliningPlan;

import com.oracle.truffle.api.frame.Frame;
import org.graalvm.compiler.truffle.common.TruffleMetaAccessProvider;

/**
 * A collection for broadcasting {@link GraalTruffleRuntimeListener} events and converting
 * {@link TruffleCompilerListener} events to {@link GraalTruffleRuntimeListener} events.
 */
@SuppressWarnings("serial")
final class GraalTruffleRuntimeListenerDispatcher extends CopyOnWriteArrayList<GraalTruffleRuntimeListener> implements GraalTruffleRuntimeListener, TruffleCompilerListener {

    @Override
    public boolean add(GraalTruffleRuntimeListener e) {
        if (e != this && !contains(e)) {
            return super.add(e);
        }
        return false;
    }

    @Override
    public void onCompilationSplit(OptimizedDirectCallNode callNode) {
        invokeListeners((l) -> l.onCompilationSplit(callNode));
    }

    @Override
    public void onCompilationSplitFailed(OptimizedDirectCallNode callNode, CharSequence reason) {
        invokeListeners((l) -> l.onCompilationSplitFailed(callNode, reason));
    }

    @Override
    public void onCompilationQueued(OptimizedCallTarget target) {
        invokeListeners((l) -> l.onCompilationQueued(target));
    }

    @Override
    public void onCompilationDequeued(OptimizedCallTarget target, Object source, CharSequence reason) {
        invokeListeners((l) -> l.onCompilationDequeued(target, source, reason));
    }

    @Override
    public void onCompilationFailed(OptimizedCallTarget target, String reason, boolean bailout, boolean permanent) {
        invokeListeners((l) -> l.onCompilationFailed(target, reason, bailout, permanent));
    }

    @Override
    public void onCompilationStarted(OptimizedCallTarget target) {
        invokeListeners((l) -> l.onCompilationStarted(target));
    }

    @Override
    public void onCompilationTruffleTierFinished(OptimizedCallTarget target, TruffleMetaAccessProvider inliningDecision, GraphInfo graph) {
        invokeListeners((l) -> l.onCompilationTruffleTierFinished(target, inliningDecision, graph));
    }

    @Override
    public void onCompilationGraalTierFinished(OptimizedCallTarget target, GraphInfo graph) {
        invokeListeners((l) -> l.onCompilationGraalTierFinished(target, graph));
    }

    @Override
    public void onCompilationSuccess(OptimizedCallTarget target, TruffleMetaAccessProvider inliningDecision, GraphInfo graph, CompilationResultInfo result) {
        invokeListeners((l) -> l.onCompilationSuccess(target, inliningDecision, graph, result));
    }

    @Override
    public void onCompilationInvalidated(OptimizedCallTarget target, Object source, CharSequence reason) {
        invokeListeners((l) -> l.onCompilationInvalidated(target, source, reason));
    }

    @Override
    public void onCompilationDeoptimized(OptimizedCallTarget target, Frame frame) {
        invokeListeners((l) -> l.onCompilationDeoptimized(target, frame));
    }

    @Override
    public void onShutdown() {
        invokeListeners((l) -> l.onShutdown());
    }

    @Override
    public void onEngineClosed(EngineData runtimeData) {
        invokeListeners((l) -> l.onEngineClosed(runtimeData));
    }

    private void invokeListeners(Consumer<? super GraalTruffleRuntimeListener> action) {
        Throwable exception = null;
        for (GraalTruffleRuntimeListener l : this) {
            try {
                action.accept(l);
            } catch (ThreadDeath t) {
                throw t;
            } catch (Throwable t) {
                if (exception == null) {
                    exception = t;
                } else {
                    exception.addSuppressed(t);
                }
            }
        }
        if (exception != null) {
            throw sthrow(RuntimeException.class, exception);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static <E extends Throwable> RuntimeException sthrow(Class<E> type, Throwable ex) throws E {
        throw (E) ex;
    }

    // Conversion from TruffleCompilerListener events to GraalTruffleRuntimeListener events

    @Override
    public void onTruffleTierFinished(CompilableTruffleAST compilable, TruffleMetaAccessProvider inliningPlan, GraphInfo graph) {
        onCompilationTruffleTierFinished((OptimizedCallTarget) compilable, inliningPlan, graph);
    }

    @Override
    public void onGraalTierFinished(CompilableTruffleAST compilable, GraphInfo graph) {
        onCompilationGraalTierFinished((OptimizedCallTarget) compilable, graph);
    }

    @Override
    public void onSuccess(CompilableTruffleAST compilable, TruffleMetaAccessProvider inliningPlan, GraphInfo graph, CompilationResultInfo result) {
        onCompilationSuccess((OptimizedCallTarget) compilable, inliningPlan, graph, result);
    }

    @Override
    public void onFailure(CompilableTruffleAST compilable, String reason, boolean bailout, boolean permanentBailout) {
        onCompilationFailed((OptimizedCallTarget) compilable, reason, bailout, permanentBailout);
    }

    @Override
    public void onCompilationRetry(CompilableTruffleAST compilable) {
        onCompilationQueued((OptimizedCallTarget) compilable);
        onCompilationStarted((OptimizedCallTarget) compilable);
    }
}
