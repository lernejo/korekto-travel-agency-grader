package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.*;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveClassLengthRule;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveMethodLengthRule;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.FieldMandatoryModifiersRule;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PartYGrader implements PartGrader<LaunchingContext> {

    @Override
    public String name() {
        return "Coding style";
    }

    @Override
    public double minGrade() {
        return -4.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        List<PmdReport> pmdReports = new PmdExecutor().runPmd(context.getExercise(),
            new Rule(
                ExcessiveClassLengthRule.class,
                "Class has {0} lines, exceeding the maximum of 80",
                Map.of("minimum", 82)
            ),
            new Rule(
                ExcessiveMethodLengthRule.class,
                "Method has {0} lines, exceeding the maximum of 15",
                Map.of("minimum", 17)
            ),
            new Rule(FieldMandatoryModifiersRule.class)
        );
        if (pmdReports.isEmpty()) {
            return new GradePart(name(), 0.0D, null, List.of("No analysis can be performed"));
        }

        List<PmdReport> sortedReports = new ArrayList<>(pmdReports);

        Collections.sort(sortedReports, Comparator.comparing(pr -> pr.getFileReports().size()));
        long violations = 0;
        List<String> messages = new ArrayList<>();

        for (PmdReport report : sortedReports) {
            List<FileReport> fileReports = new ArrayList<>(report.getFileReports());
            Collections.sort(fileReports, Comparator.comparing(fr -> fr.getName()));
            violations += fileReports.stream().mapToLong(fr -> fr.getViolations().size()).sum();
            fileReports
                .stream()
                .map(fr -> fr.getName() + buildViolationsBlock(fr))
                .forEach(messages::add);
        }

        if (messages.isEmpty()) {
            messages.add("OK");
        }
        return new GradePart(name(), Math.max(violations * minGrade() / 4, minGrade()), null, messages);
    }

    @NotNull
    private String buildViolationsBlock(FileReport fr) {
        List<Violation> violations = new ArrayList<>(fr.getViolations());
        Collections.sort(violations, Comparator.<Violation, Integer>comparing(v -> v.getBeginLine()).thenComparing(v -> v.getBeginColumn()));
        return fr.getViolations()
            .stream()
            .map(v -> "L." + v.getBeginLine() + ": " + v.getMessage().trim())
            .collect(Collectors.joining("\n            * ", "\n            * ", ""));
    }
}
