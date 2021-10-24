# AndroidSignalServerBarnaul
Сервер коммутатор запросов через белы IP адрес




Оставить сообщение для устройства с именем "DeviceName1"
```
http://128.0.24.172:8266/push=DeviceName1&msg=Сообщение для устройства
```

Забрать сообщение оставленое ранее для устройства "DeviceName1"
```
http://128.0.24.172:8266/pop=DeviceName1
```

Отправить сообщение для устройства, которое подключено через сокет соединение
```
http://128.0.24.172:8266/send=DeviceName1&txt=Сообщение для устройства через сокет подключение
```



Кроссдоменный запрос через JS.
```html
<html>
	<script type="text/javascript" >
		var xhr = new XMLHttpRequest()
		xhr.open( 'GET','http://128.0.24.172:8266/pop=test', true )
		xhr.onreadystatechange = function() {
		  if (xhr.readyState != 4) { return; }
		  if (xhr.status === 200) {
			console.log('result', xhr.responseText)
		  } else {
			console.log('err', xhr.responseText)
		  }
		}
		xhr.send()
	</script>
</html>
```

Подключение через ROW socket
```
{"socket":"test"}
```


```
{"push":"test2","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}
{"exit":"exit"}
exit
list
{"pop":"test"}
{"send":"test","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}
{"send":"test3","msg":"sadflkjasdl;fjas;ldfjl;asjfl;as"}
```

