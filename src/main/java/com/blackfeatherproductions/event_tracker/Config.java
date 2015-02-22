package com.blackfeatherproductions.event_tracker;

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
    
    private Integer maxFailures;
    
    //Database Properties
    @Deprecated
    private Integer dbConnectionLimit;
    @Deprecated
    private String dbHost;
    @Deprecated
    private String dbUser;
    @Deprecated
    private String dbPassword;
    @Deprecated
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
            
            maxFailures = Integer.valueOf(prop.getProperty("maxFailures", "20"));
            
            //Database Properties
            dbConnectionLimit = Integer.valueOf(prop.getProperty("dbConnectionLimit", "100"));
            dbHost = prop.getProperty("dbHost", "127.0.0.1");
            dbUser = prop.getProperty("dbUser", "eventTracker");
            dbPassword = prop.getProperty("dbPassword", "password");
            dbName = prop.getProperty("dbName", "ps2_events");
            
            //API Database Properties
            apiDbConnectionLimit = Integer.valueOf(prop.getProperty("apiDbConnectionLimit", "100"));
            apiDbHost = prop.getProperty("apiDbHost", "127.0.0.1");
            apiDbUser = prop.getProperty("apiDbUser", "api");
            apiDbPassword = prop.getProperty("apiDbPassword", "password");
            apiDbName = prop.getProperty("apiDbName", "api");
     
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

    @Deprecated
    public Integer getDbConnectionLimit()
    {
        return dbConnectionLimit;
    }

    @Deprecated
    public String getDbHost()
    {
        return dbHost;
    }

    @Deprecated
    public String getDbUser()
    {
        return dbUser;
    }

    @Deprecated
    public String getDbPassword()
    {
        return dbPassword;
    }

    @Deprecated
    public String getDbName()
    {
        return dbName;
    }

    public Integer getMaxFailures()
    {
		return maxFailures;
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
