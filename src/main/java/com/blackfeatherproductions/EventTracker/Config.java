package com.blackfeatherproductions.EventTracker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config
{
    //SOE Service ID
    private String soeServiceID;
    
    //Server Listen Port
    private Integer serverPort;
    
    //Database Properties
    private Integer dbConnectionLimit;
    private String dbHost;
    private String dbUser;
    private String dbPassword;
    private String dbName;
    
    //API Database Properties
    private Integer apiDbConnectionLimit;
    private String apiDbHost;
    private String apiDbUser;
    private String apiDbPassword;
    private String apiDbName;
    
    public Config()
    {
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
            
            //Database Properties
            dbConnectionLimit = Integer.valueOf(prop.getProperty("dbConnectionLimit", "100"));
            dbHost = prop.getProperty("dbHost", "127.0.0.1");
            dbUser = prop.getProperty("dbUser", "eventTracker");
            dbPassword = prop.getProperty("dbPassword", "password");
            dbName = prop.getProperty("dbName", "ps2_events");
            
            //API Database Properties
            apiDbConnectionLimit = Integer.valueOf(prop.getProperty("dbConnectionLimit", "100"));
            apiDbHost = prop.getProperty("dbHost", "127.0.0.1");
            apiDbUser = prop.getProperty("dbUser", "api");
            apiDbPassword = prop.getProperty("dbPassword", "password");
            apiDbName = prop.getProperty("dbName", "api");
     
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

    public Integer getApiDbConnectionLimit()
    {
        return apiDbConnectionLimit;
    }

    public String getApiDbHost()
    {
        return apiDbHost;
    }

    public String getApiDbUser()
    {
        return apiDbUser;
    }

    public String getApiDbPassword()
    {
        return apiDbPassword;
    }

    public String getApiDbName()
    {
        return apiDbName;
    }
}
