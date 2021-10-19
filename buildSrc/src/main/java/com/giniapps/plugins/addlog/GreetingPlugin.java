package com.giniapps.plugins.addlog;

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


public class GreetingPlugin implements Plugin<Project> {

    Project project;

    @Override
    public void apply(Project project) {
      
        project.task("hello", task -> {
            System.out.println("Hello from greeeting plugin !!!!");
            System.out.println("Hello from greeeting plugin second !!!!");
        });

        this.project = project;

        registerTransformer();
    }

    void registerTransformer() {
        Object android = project.findProperty("android");

        if (android instanceof BaseAppModuleExtension) {
            System.err.println("Adding transform");
            ((BaseAppModuleExtension) android).registerTransform(new CustomTransform());
        }
    }



}
