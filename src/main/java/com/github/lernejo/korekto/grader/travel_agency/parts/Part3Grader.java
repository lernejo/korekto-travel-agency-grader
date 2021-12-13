package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.misc.Maths;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenJacocoReport;

import java.util.List;

public class Part3Grader implements PartGrader {

    @Override
    public String name() {
        return "Part 3 - Code Coverage";
    }

    @Override
    public Double maxGrade() {
        return 4.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.testFailed()) {
            return result(List.of("Coverage not available when there is test failures"), 0.0D);
        }
        List<MavenJacocoReport> jacocoReports = MavenJacocoReport.from(context.getExercise());

        if (jacocoReports.isEmpty()) {
            return result(List.of("No JaCoCo report produced after `mvn verify`, check tests and plugins"), 0D);
        } else {
            MavenJacocoReport mergedReport = MavenJacocoReport.merge(jacocoReports);
            double ratio = mergedReport.getRatio();
            if (ratio < 0.75D) {
                double grade = Maths.round((ratio * maxGrade()) / 0.75D, 2);
                return result(List.of("Code coverage: " + Maths.round(ratio * 100, 2) + "%, expected: > 80% with `mvn verify`"), grade);
            } else {
                return result(List.of(), maxGrade());
            }
        }
    }
}
