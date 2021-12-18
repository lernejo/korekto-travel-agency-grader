package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import com.github.lernejo.korekto.toolkit.thirdparty.git.MeaninglessCommit;

import java.util.List;
import java.util.stream.Collectors;

public class PartXGrader implements PartGrader<LaunchingContext> {

    @Override
    public String name() {
        return "Git (proper descriptive messages)";
    }

    @Override
    public double minGrade() {
        return -4.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        GitContext gitContext = context.getExercise().lookupNature(GitNature.class).get().getContext();
        List<MeaninglessCommit> meaninglessCommits = gitContext.meaninglessCommits();
        List<String> messages = meaninglessCommits.stream()
            .map(mc -> '`' + mc.getShortId() + "` " + mc.getMessage() + " --> " + mc.getReason())
            .collect(Collectors.toList());
        if (messages.isEmpty()) {
            messages.add("OK");
        }
        return new GradePart(name(), Math.max(meaninglessCommits.size() * minGrade() / 8, minGrade()), null, messages);
    }
}
