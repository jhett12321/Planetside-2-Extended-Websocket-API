package com.blackfeatherproductions.event_tracker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

public class Config
{
    //SOE Service ID
    private String soeServiceID;

    //Server Listen Port
    private Integer serverPort;

    private Integer maxFailures;

    //Database Properties
    private Integer dbConnectionLimit;
    private String dbHost;
    private String dbUser;
    private String dbPassword;
    private String dbName;

    public Config()
    {
        File properties = new File("eventTracker.properties");
        
        if(!properties.exists())
        {
            EventTracker.getLogger().warn("No Config Found! Generating new default config (eventTracker.properties).");
            EventTracker.getLogger().warn("It is strongly recommended that you update your database info and DGC service ID's before continuing use.");
            
            InputStream defaultProperties = (getClass().getResourceAsStream("/defaults/eventTracker.properties"));
            try
            {
                Files.copy(defaultProperties, properties.getAbsoluteFile().toPath());
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        
        Properties prop = new Properties();
        InputStream input = null;

        try
        {
            input = new FileInputStream("eventTracker.properties");

            prop.load(input);

            //SOE Service ID
            soeServiceID = prop.getProperty("soeServiceID", "example");

            //Server Listen Port
            serverPort = Integer.valueOf(prop.getProperty("serverPort", "8080"));
            maxFailures = Integer.valueOf(prop.getProperty("maxFailures", "20"));

            //Database Properties
            dbConnectionLimit = Integer.valueOf(prop.getProperty("dbConnectionLimit", "100"));
            dbHost = prop.getProperty("dbHost", "127.0.0.1");
            dbUser = prop.getProperty("dbUser", "eventTracker");
            dbPassword = prop.getProperty("dbPassword", "password");
            dbName = prop.getProperty("dbName", "ps2_events");
        }

        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public Integer getServerPort()
    {
        return serverPort;
    }

    public String getSoeServiceID()
    {
        return soeServiceID;
    }

    public Integer getMaxFailures()
    {
        return maxFailures;
    }

    public Integer getDbConnectionLimit()
    {
        return dbConnectionLimit;
    }

    public String getDbHost()
    {
        return dbHost;
    }

    public String getDbUser()
    {
        return dbUser;
    }

    public String getDbPassword()
    {
        return dbPassword;
    }

    public String getDbName()
    {
        return dbName;
    }
}