package at.nieslony.arachne;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:git.properties", ignoreResourceNotFound = true)
@Getter
public class ArachneVersion extends ArachneVersionBase {

    @Value("${git.tags:}")
    private String gitTag;

    @Value("${git.closest.tag.commit.count:}")
    private String gitClosestTagCommit;

    @Value("${git.build.version:}")
    private String gitBuildVersion;

    @Value("${git.commit.user.name:}")
    private String gitCommitUserName;

    @Value("${git.commit.id.abbrev:}")
    private String gitCommitIdAbbrev;

    @Value("${git.branch:}")
    private String gitBranch;

    @Value("${git.build.host:}")
    private String gitBuildHost;

    @Value("git.commit.id.describe-short:}")
    private String gitCommitIdDescribeShort;

    @Value("${git.commit.id.describe:}")
    private String gitCommitIdDescribe;

    @Value("${git.build.user.email:}")
    private String gitBuildUserEmail;

    @Value("${git.commit.id:}")
    private String gitCommitId;

    @Value("${git.commit.message.short:}")
    private String gitCommitMessageShort;

    @Value("${git.commit.user.email:}")
    private String gitCommitUserEmail;

    @Value("${git.closest.tag.name:}")
    private String gitClosestTagName;

    @Value("${git.commit.time:}")
    private String gitCommitTime;

    @Value("${git.build.time:}")
    private String gitBuildTime;

    @Value("${git.build.user.name:}")
    private String gitBuildUserName;

    @Value("${git.dirty:}")
    private String gitDirty;

    @Value("${git.commit.message.full:}")
    private String gitCommitMessageFull;

    @Value("${git.remote.origin.url:}")
    private String gitRemoteOriginUrl;

    public String getPrettyVersion() {
        if (gitTag != null && !gitTag.isEmpty()) {
            return gitTag;
        }

        if (gitBranch == null || gitBranch.isEmpty()) {
            return ARACHNE_VERSION;
        }

        // "1.3.1-7-master+gitXXX"
        return "%s-%s-git%s-%s".formatted(
                gitClosestTagName,
                gitClosestTagCommit,
                gitCommitIdAbbrev,
                gitBranch
        );
    }
}
