'use strict';

//const vConsole = new VConsole();
//const remoteConsole = new RemoteConsole("http://[remote server]/logio-post");
//window.datgui = new dat.GUI();

const base_url = "";

var vue_options = {
    el: "#top",
    mixins: [mixins_bootstrap],
    store: vue_store,
    data: {
        target_item: {},
        uuid_list: [],
        widget_list: [],
        target_widget: {},
        widget_update: {},
    },
    computed: {
    },
    methods: {
        delete_uuid_call: async function(){
            if( !this.target_item.uuid )
                return;
            if( !confirm("本当に削除しますか？") )
                return;
            
            var result = await do_post(base_url + "/widget-delete-uuid", { uuid: this.target_item.uuid });
            console.log(result);
            this.uuid_list_update();
            alert('削除しました。');
        },
        uuid_list_update: async function(){
            var result = await do_post(base_url + "/widget-list-uuid");
            console.log(result);
            this.uuid_list = result.rows;
        },
        widget_list_update: async function(){
            var result = await do_post(base_url + "/widget-list", { uuid: this.target_item.uuid });
            console.log(result);
            this.widget_list = result.rows;
        },
        show_payload: function(index){
            this.target_widget = this.widget_list[index];
            this.dialog_open('#show_payload_dialog');
        },
        delete_widget_call: async function(index){
            if( !confirm("本当に削除しますか？") )
                return;
            
            var result = await do_post(base_url + "/widget-delete", { uuid: this.target_item.uuid, widget_id: this.widget_list[index].widget_id });
            console.log(result);
            this.widget_list_update();
            alert('削除しました。');
        },
        update_widget_start: function(index){
            this.widget_update = JSON.parse(JSON.stringify(this.widget_list[index]));
            this.dialog_open('#update_widget_dialog');
        },
        update_widget_call: async function(){
            try{
                var json = JSON.parse(this.widget_update.payload);
                console.log(json);
            }catch(error){
                alert(error);
                return;
            }
            var params = {
                uuid: this.widget_update.uuid,
                widget_id: this.widget_update.widget_id,
                target_url: this.widget_update.target_url,
                payload: this.widget_update.payload
            };
            var result = await do_post(base_url + "/widget-update", params);
            console.log(result);
            this.dialog_close('#update_widget_dialog');
            this.widget_list_update();
            alert('更新しました。');
        }
    },
    created: function(){
    },
    mounted: function(){
        proc_load();

        this.uuid_list_update();
    }
};
vue_add_data(vue_options, { progress_title: '' }); // for progress-dialog
vue_add_global_components(components_bootstrap);
vue_add_global_components(components_utils);

/* add additional components */
  
window.vue = new Vue( vue_options );
