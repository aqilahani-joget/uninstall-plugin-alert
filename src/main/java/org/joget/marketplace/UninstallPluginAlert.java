package org.joget.marketplace;

import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joget.apps.app.model.UiHtmlInjectorPluginAbstract;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;

public class UninstallPluginAlert extends UiHtmlInjectorPluginAbstract implements PluginWebSupport {
       
    @Override
    public String getName() {
        return "Uninstall Plugin Alert";
    }

    @Override
    public String getVersion() {
        return "8.2.0";
    }

    @Override
    public String getDescription() {
        return "UI Html Injector plugin to alert users the plugin is being used in which app before uninstalling.";
    }

    @Override
    public String[] getInjectUrlPatterns() {
        return new String[] {"/web/console/setting/plugin"};
    }
    
    @Override
    public String getHtml(HttpServletRequest request) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Map data = new HashMap();
        data.put("plugin", this);
        data.put("request", request);
        
        return pluginManager.getPluginFreeMarkerTemplate(data, getClassName(), "/templates/UninstallPluginAlertUiHtmlInjector.ftl", null);
    }

    @Override
    public boolean isIncludeForAjaxThemePageSwitching() {
        return false;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String body = request.getReader().lines().collect(Collectors.joining());
        JSONObject json = new JSONObject(body);
        JSONArray pluginClasses = json.getJSONArray("selectedList");
        
        List<Map<String, String>> apps = new ArrayList<>();
        for (Object p: pluginClasses.toList()) {
            LogUtil.info(getClassName(), "plugin class:" + p.toString());
            apps = executeQery(p.toString(), apps);
            LogUtil.info(getClassName(), apps.toString());
        }

        List<String> appIds = new ArrayList<String>();
        List<String> appNames = new ArrayList<String>();

        for (Map<String, String> map : apps) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                appIds.add(entry.getKey());    
                appNames.add(entry.getValue()); 
            }
        }
        
        Map<String, Object> jsonOutput = new HashMap<String, Object>();
        jsonOutput.put("ids", appIds);
        jsonOutput.put("names", appNames);

        PrintWriter out = response.getWriter();
        out.print(new ObjectMapper().writeValueAsString(jsonOutput)); // use Jackson for proper JSON
        out.flush();
    }

    public List<Map<String, String>> executeQery(String pluginClass, List<Map<String, String>> apps) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        String sql = getSql();
        try {
            DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            PreparedStatement ps = con.prepareStatement(sql);

            String searchPattern = "%" + pluginClass + "%"; // your search string
            int i = 1;
            ps.setString(i++, searchPattern); // builder.json LIKE ?
            ps.setString(i++, searchPattern); // form.json LIKE ?
            ps.setString(i++, searchPattern); // datalist.json LIKE ?
            ps.setString(i++, searchPattern); // userview.json LIKE ?
            ps.setString(i++, pluginClass);      // plugin_default.id = ?
            ps.setString(i++, searchPattern); // rsc.permissionProperties LIKE ?
            ps.setString(i++, searchPattern); // pkg_act_plugin.pluginProperties LIKE ?
            ps.setString(i++, searchPattern); // pkg_participant.pluginProperties LIKE ?

            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, String> appIdName = new HashMap<>();
                // Get columns by name
                String appId = rs.getString("appId");
                String appName = rs.getString("name");

                appIdName.put(appId, appName);

                boolean exists = apps.stream()
                             .anyMatch(map -> appName.equals(map.get(appId)));

                if (!exists) {
                    apps.add(appIdName);
                }
            }

        } catch (SQLException | BeansException e) {
            LogUtil.error(getClassName(), e, "");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "");
            }
        }
        
        return apps;
    }

    private String getSql() {
        return "SELECT app.appId, app.name " +
                "FROM app_app app " +
                "WHERE app.published = 1 " +
                "AND EXISTS ( " +
                "    SELECT 1 FROM ( " +
                "        SELECT appId FROM app_builder " +
                "            WHERE app_builder.appId = app.appId " +
                "              AND app_builder.appVersion = app.appVersion " +
                "              AND app_builder.json LIKE ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_form " +
                "            WHERE app_form.appId = app.appId " +
                "              AND app_form.appVersion = app.appVersion " +
                "              AND app_form.json LIKE ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_datalist " +
                "            WHERE app_datalist.appId = app.appId " +
                "              AND app_datalist.appVersion = app.appVersion " +
                "              AND app_datalist.json LIKE ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_userview " +
                "            WHERE app_userview.appId = app.appId " +
                "              AND app_userview.appVersion = app.appVersion " +
                "              AND app_userview.json LIKE ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_plugin_default " +
                "            WHERE app_plugin_default.appId = app.appId " +
                "              AND app_plugin_default.appVersion = app.appVersion " +
                "              AND app_plugin_default.id = ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_resource " +
                "            WHERE app_resource.appId = app.appId " +
                "              AND app_resource.appVersion = app.appVersion " +
                "              AND app_resource.permissionProperties LIKE ? " +
                "        UNION ALL " +
                "        SELECT p.appId FROM app_package_activity_plugin " +
                "            INNER JOIN app_package p ON p.packageId = app_package_activity_plugin.packageId " +
                "              AND p.packageVersion = app_package_activity_plugin.packageVersion " +
                "            WHERE p.appId = app.appId " +
                "              AND p.appVersion = app.appVersion " +
                "              AND app_package_activity_plugin.pluginProperties LIKE ? " +
                "        UNION ALL " +
                "        SELECT appId FROM app_package_participant " +
                "            INNER JOIN app_package p ON p.packageId = app_package_participant.packageId " +
                "              AND p.packageVersion = app_package_participant.packageVersion " +
                "            WHERE p.appId = app.appId " +
                "              AND p.appVersion = app.appVersion " +
                "              AND app_package_participant.pluginProperties LIKE ? " +
                "    ) AS tmp " +  
                ")";
    }
}
