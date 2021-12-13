package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.grader.travel_agency.parts.*;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.Grader;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SubjectForToolkitInclusion
public class TravelAgencyGrader implements Grader<LaunchingContext> {

    private final Logger logger = LoggerFactory.getLogger(TravelAgencyGrader.class);

    public static final int SITE_PORT = 8085;
    public static final int PREDICTION_ENGINE_PORT = 8075;

    private final SilentJacksonConverterFactory jacksonConverterFactory = SilentJacksonConverterFactory.create();

    private final TravelAgencyApiClient travelAgencyApiClient = new Retrofit.Builder()
        .baseUrl("http://localhost:" + SITE_PORT + "/")
        .addConverterFactory(jacksonConverterFactory)
        .build()
        .create(TravelAgencyApiClient.class);

    private final PredictionApiClient predictionApiClient = new Retrofit.Builder()
        .baseUrl("http://localhost:" + PREDICTION_ENGINE_PORT + "/")
        .addConverterFactory(jacksonConverterFactory)
        .build()
        .create(PredictionApiClient.class);

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
        return new LaunchingContext(configuration, travelAgencyApiClient, predictionApiClient, jacksonConverterFactory::newExceptionHolder);
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

    private Collection<? extends PartGrader> graders() {
        return List.of(
            new Part1Grader(),
            new Part2Grader(),
            new Part3Grader(),
            new Part4Grader()
        );
    }
}
