<script>
    window.uninstall = function(selectedList) {
        console.log(selectedList);
        $.ajax({
            url: "/jw/web/json/plugin/org.joget.marketplace.UninstallPluginAlert/service",
            type: "POST",
            contentType: "application/json",
            dataType: "json",
            data: JSON.stringify({ selectedList: selectedList}),
            success: function (response) {
              var plugins = response.names || []; // already an array
              var bulletList = "";
              if (plugins.length > 0) {
                // Build bullet list
                bulletList = "\n There are apps using this plugin(s):\n ";
                for (var i = 0; i < plugins.length; i++) {
                    bulletList += "• " + plugins[i] + "\n ";
                }
              }
              // console.log(decodeURIComponent(response.jars));
              if (confirm('Are you sure you want to uninstall the selected Plugin(s)? ' + bulletList)) {
                  UI.blockUI(); 
                  var callback = {
                      success: function() {
                          document.location = '/jw/web/console/setting/plugin';
                      }
                  };
                  var request = ConnectionManager.post(
                      '/jw/web/console/setting/plugin/uninstall',
                      callback,
                      'selectedPlugins=' + selectedList
                  );
              }
            },
            error: function (xhr, status, error) {
            }
        });
    };
</script>
