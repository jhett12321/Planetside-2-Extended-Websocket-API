package com.blackfeatherproductions.event_tracker;

public final class MavenInfo
{
    private static final String VERSION = "${project.version}";
    private static final String GROUPID = "${project.groupId}";
    private static final String ARTIFACTID = "${project.artifactId}";

    public static String getVersion()
    {
        return VERSION;
    }

    public static String getGroupID()
    {
        return GROUPID;
    }

    public static String getArtifactID()
    {
        return ARTIFACTID;
    }
}
