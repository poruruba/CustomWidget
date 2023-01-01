'use strict';

const HELPER_BASE = process.env.HELPER_BASE || "/opt/";
const Response = require(HELPER_BASE + 'response');
const Redirect = require(HELPER_BASE + 'redirect');

const sqlite3 = require("sqlite3");
const WIDGET_FILE_PATH = process.env.THIS_BASE_PATH + '/data/widget/widget.db';
const WIDGET_TABLE_NAME = "widget";

const { URL, URLSearchParams } = require('url');
const fetch = require('node-fetch');
const Headers = fetch.Headers;

const db = new sqlite3.Database(WIDGET_FILE_PATH);
db.each("SELECT COUNT(*) FROM sqlite_master WHERE TYPE = 'table' AND name = '" + WIDGET_TABLE_NAME + "'", (err, row) =>{
  if( err ){
    console.error(err);
    return;
  }
  if( row["COUNT(*)"] == 0 ){
    db.run("CREATE TABLE '" + WIDGET_TABLE_NAME + "' (id TEXT PRIMARY KEY, uuid TEXT, widget_id INTEGER, title TEXT, model TEXT, target_url TEXT, payload TEXT)", (err, row) =>{
      if( err ){
        console.error(err);
        return;
      }
    });
  }
});

exports.handler = async (event, context, callback) => {
	var body = JSON.parse(event.body);
	console.log(body);
	
	if( event.path == '/widget-add' ){
		var uuid = body.uuid;
		var widget_id = body.widget_id;
		var title = body.title;
		var model = body.model;
		var id = uuid + '_' + widget_id;
		var result = await new Promise((resolve, reject) =>{
			db.run("INSERT INTO '" + WIDGET_TABLE_NAME + "' (id, uuid, widget_id, title, model) VALUES (?, ?, ?, ?, ?)", [id, uuid, widget_id, title, model], (err) =>{
				if( err )
					return reject(err);
				resolve({});
			});
		});

		return new Response({});
	}else
	if( event.path == '/widget-update' ){
		var uuid = body.uuid;
		var widget_id = body.widget_id;
		var target_url = body.target_url;
		var payload = body.payload;
		var id = uuid + '_' + widget_id;
		var result = await new Promise((resolve, reject) =>{
			db.run("UPDATE '" + WIDGET_TABLE_NAME + "' SET target_url = ?, payload = ? WHERE id = ?", [target_url, payload, id], (err) =>{
				if( err )
					return reject(err);
				resolve({});
			});
		});

		return new Response({});
	}else
	if( event.path == '/widget-delete' ){
		var uuid = body.uuid;
		var widget_id = body.widget_id;
		var id = uuid + '_' + widget_id;
	    var result = await new Promise((resolve, reject) =>{
	      db.all("DELETE FROM '" + WIDGET_TABLE_NAME + "' WHERE id = ?", [id], (err) => {
				if( err )
					return reject(err);
				resolve({});
			});
	    });
		return new Response({});
	}else
	if( event.path == '/widget-call' ){
		var uuid = body.uuid;
		var widget_id = body.widget_id;
		var id = uuid + '_' + widget_id;
	    var item = await new Promise((resolve, reject) =>{
	      db.all("SELECT * FROM '" + WIDGET_TABLE_NAME + "' WHERE id = ?", [id], (err, rows) => {
				if( err )
					return reject(err);
				if( rows.length > 0 )
					resolve(rows[0]);
				else
					reject('id not found');
	      });
	    });
	    console.log(item);
	    
	    if( item.target_url ){
	    	var result = await do_post(item.target_url, JSON.parse(item.payload));
	    	return new Response({ result: result });
	    }else{
			return new Response({});
		}
	}else
	if( event.path == '/widget-list' ){
		var uuid = body.uuid;
	    var rows = await new Promise((resolve, reject) =>{
	      db.all("SELECT * FROM '" + WIDGET_TABLE_NAME + "' WHERE uuid = ?", [uuid], (err, rows) => {
				if( err )
					return reject(err);
				resolve(rows);
	      });
	    });

		return new Response({ rows: rows });
	}else
	if( event.path == '/widget-list-uuid' ){
	    var rows = await new Promise((resolve, reject) =>{
	      db.all("SELECT DISTINCT uuid, model FROM '" + WIDGET_TABLE_NAME + "'", [], (err, rows) => {
				if( err )
					return reject(err);
				resolve(rows);
	      });
	    });

		var list = rows.map(item => {
		 return { uuid: item.uuid, model: item.model };
		});
		return new Response({ rows: list });
	}else
	if( event.path == '/widget-delete-uuid' ){
		var uuid = body.uuid;
	    var result = await new Promise((resolve, reject) =>{
	      db.all("DELETE FROM '" + WIDGET_TABLE_NAME + "' WHERE uuid = ?", [uuid], (err) => {
				if( err )
					return reject(err);
				resolve({});
			});
	    });

		return new Response({});
	}else{
		throw "unknown endpoint";
	}
};

function do_post(url, body) {
  const headers = new Headers({ "Content-Type": "application/json" });

  return fetch(url, {
      method: 'POST',
      body: JSON.stringify(body),
      headers: headers
    })
    .then((response) => {
      if (!response.ok)
        throw new Error('status is not 200');
      return response.json();
    });
}