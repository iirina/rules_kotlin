/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.ruleskotlin.workers.compilers.jvm;


import io.bazel.ruleskotlin.workers.*;
import io.bazel.ruleskotlin.workers.compilers.jvm.actions.*;

import java.io.IOException;
import java.util.List;

/**
 * Bazel Kotlin Compiler worker.
 */
public final class KotlinJvmBuilder implements CommandLineProgram {
    private final BuildAction[] compileActions;

    private KotlinJvmBuilder() {
        KotlinToolchain kotlinToolchain;
        try {
            kotlinToolchain = new KotlinToolchain();
        } catch (IOException e) {
            throw new RuntimeException("could not initialize toolchain", e);
        }

        compileActions = new BuildAction[]{
                Initialize.INSTANCE,
                new KotlinMainCompile(kotlinToolchain),
                new JavaMainCompile(),
                KotlinRenderClassCompileResult.INSTANCE,
                CreateOutputJar.INSTANCE,
                GenerateJdepsFile.INSTANCE,
        };
    }

    @Override
    public Integer apply(List<String> args) {
        Context context = Context.from(args);
        Integer exitCode = 0;
        for (BuildAction action : compileActions) {
            exitCode = action.apply(context);
            if (exitCode != 0)
                break;
        }
        return exitCode;
    }

    public static void main(String[] args) {
        KotlinJvmBuilder kotlinBuilder = new KotlinJvmBuilder();
        BazelWorker<KotlinJvmBuilder> kotlinCompilerBazelWorker = new BazelWorker<>(
                kotlinBuilder,
                System.err,
                "KotlinCompile"
        );
        System.exit(kotlinCompilerBazelWorker.apply(args));
    }
}
