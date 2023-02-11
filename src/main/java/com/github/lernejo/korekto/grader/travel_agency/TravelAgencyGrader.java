package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.grader.travel_agency.parts.Part1Grader;
import com.github.lernejo.korekto.grader.travel_agency.parts.Part4Grader;
import com.github.lernejo.korekto.grader.travel_agency.parts.Part5Grader;
import com.github.lernejo.korekto.grader.travel_agency.parts.Part6Grader;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.Grader;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import com.github.lernejo.korekto.toolkit.partgrader.GitHistoryPartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.GitHubActionsPartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.JacocoCoveragePartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.PmdPartGrader;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@SubjectForToolkitInclusion
public class TravelAgencyGrader implements Grader<LaunchingContext> {

    static {
        Locale.setDefault(Locale.US);
    }

    private final Logger logger = LoggerFactory.getLogger(TravelAgencyGrader.class);

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/travel_agency";
    }

    @Override
    public boolean needsWorkspaceReset() {
        return true;
    }

    @Override
    public void run(LaunchingContext context) {
        Optional<GitNature> optionalGitNature = context.getExercise().lookupNature(GitNature.class);
        if (optionalGitNature.isEmpty()) {
            context.getGradeDetails().getParts().add(new GradePart("exercise", 0D, 12D, List.of("Not a Git project")));
        } else {
            context.getGradeDetails().getParts().addAll(grade(context));
        }
    }

    @Override
    public LaunchingContext gradingContext(GradingConfiguration configuration) {
        return new LaunchingContext(configuration);
    }

    private Collection<? extends GradePart> grade(LaunchingContext context) {
        return graders().stream()
            .map(g -> applyPartGrader(context, g))
            .collect(Collectors.toList());
    }

    private GradePart applyPartGrader(LaunchingContext context, PartGrader g) {
        long startTime = System.currentTimeMillis();
        try {
            return g.grade(context);
        } finally {
            logger.debug(g.name() + " in " + HumanReadableDuration.toString(System.currentTimeMillis() - startTime));
        }
    }

    private Collection<? extends PartGrader<LaunchingContext>> graders() {
        return List.of(
            new Part1Grader(),
            new GitHubActionsPartGrader<>("Part 2 - CI", 2.0D),
            new JacocoCoveragePartGrader<>("Part 3 - Code Coverage", 4.0D, 0.8D),
            new Part4Grader(),
            new Part5Grader(),
            new Part6Grader(TravelAgencyApiClient.WeatherExpectation.COLDER),
            new Part6Grader(TravelAgencyApiClient.WeatherExpectation.WARMER),
            new GitHistoryPartGrader<>("Git (proper descriptive messages)", -4.0D),
            new PmdPartGrader<>("Coding style", -15.0D,-1.0D,
                Rule.buildExcessiveClassLengthRule(80),
                Rule.buildExcessiveMethodLengthRule(15),
                Rule.buildFieldMandatoryModifierRule(0, "private", "final", "!static"),
                Rule.buildClassNamingConventionsRule(),
                Rule.buildMethodNamingConventionsRule(),
                Rule.buildDependencyInversionRule(),
                Rule.buildUnusedPrivateMethodRule(),
                Rule.buildUnusedPrivateFieldRule(),
                Rule.buildUnusedLocalVariableRule(),
                Rule.buildEmptyControlStatementRule()
            )
        );
    }
}
