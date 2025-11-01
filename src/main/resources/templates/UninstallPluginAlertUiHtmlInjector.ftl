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

              // Build bullet list
              var bulletList = "";
              for (var i = 0; i < plugins.length; i++) {
                  bulletList += "• " + plugins[i] + "\n ";
              }
              if (confirm('Are you sure you want to uninstall the selected Plugin(s)? \n There are apps using this plugins(s):\n ' + bulletList)) {
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
