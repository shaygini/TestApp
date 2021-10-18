package com.giniapps.plugins.addlog;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GreetingPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.task("hello", task -> {
            System.out.println("Hello from greeeting plugin !!!!");
            System.out.println("Hello from greeeting plugin second !!!!");
        });
    }
}
