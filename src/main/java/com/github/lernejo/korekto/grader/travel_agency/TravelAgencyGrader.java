package com.github.lernejo.korekto.grader.travel_agency;

import com.github.lernejo.korekto.grader.travel_agency.parts.Part1Grader;
import com.github.lernejo.korekto.grader.travel_agency.parts.Part2Grader;
import com.github.lernejo.korekto.grader.travel_agency.parts.PartGrader;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.Grader;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration;
import com.github.lernejo.korekto.toolkit.misc.SubjectForToolkitInclusion;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SubjectForToolkitInclusion
public class TravelAgencyGrader implements Grader<LaunchingContext> {

    private final Logger logger = LoggerFactory.getLogger(TravelAgencyGrader.class);

    private final Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://localhost:8085/")
        .addConverterFactory(JacksonConverterFactory.create())
        .build();

    public final TravelAgencyApiClient client = retrofit.create(TravelAgencyApiClient.class);

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
        return new LaunchingContext(configuration, client);
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
            new Part2Grader()
        );
    }
}
