package com.github.lernejo.korekto.grader.travel_agency.parts;

import com.github.lernejo.korekto.grader.travel_agency.LaunchingContext;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import com.github.lernejo.korekto.toolkit.thirdparty.github.GitHubNature;
import com.github.lernejo.korekto.toolkit.thirdparty.github.WorkflowRun;
import com.github.lernejo.korekto.toolkit.thirdparty.github.WorkflowRunConclusion;
import com.github.lernejo.korekto.toolkit.thirdparty.github.WorkflowRunStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Part2Grader implements PartGrader {
    private static final Set<String> mainBranchNames = Set.of("main", "master");

    @Override
    public String name() {
        return "Part 2 - CI";
    }

    @Override
    public Double maxGrade() {
        return 2.0D;
    }

    @Override
    public GradePart grade(LaunchingContext context) {
        GitNature gitNature = context.getExercise().lookupNature(GitNature.class).get();
        String branch = gitNature.getContext().currentBranchName();
        Optional<GitHubNature> gitHubNature = context.getExercise().lookupNature(GitHubNature.class);
        if (gitHubNature.isEmpty()) {
            return result(List.of("Not a GitHub project"), 0D);
        }
        List<WorkflowRun> actionRuns = gitHubNature.get().listActionRuns();

        List<WorkflowRun> mainRuns = actionRuns.stream()
            .filter(wr -> wr.getStatus() == WorkflowRunStatus.completed)
            .filter(wr -> branch.equals(wr.getHead_branch()))
            .collect(Collectors.toList());
        if (mainRuns.isEmpty()) {
            return result(List.of("No CI runs for `" + branch + "` branch, check https://github.com/" + context.getExercise().getName() + "/actions"), 0D);
        } else {
            WorkflowRun latestRun = mainRuns.get(0);
            if (latestRun.getConclusion() != WorkflowRunConclusion.success) {
                return result(List.of("Latest CI run of branch `" + branch + "` was expected to be in *success* state but found: " + latestRun.getConclusion()), maxGrade() / 2);
            } else {
                return result(List.of(), maxGrade());
            }
        }
    }
}
