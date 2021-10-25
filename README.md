# AndroidSignalServerBarnaul
Сервер коммутатор запросов через белы IP адрес


Сервер коммутатор работает по принципу описанному ниже
```
В университете начинается лекция по психологии. Удобно устроившись за кафедрой, профессор:
    - Сегодня, товарищи студенты, мы будем с вами изучать три сходные стадии психики человека: удивление, раздражение и гнев. Рассмотрим на конкретном примере...
    Достав из портфеля телефон, профессор набирает первый попавшийся номер.
    - Здравствуйте, а Васю можно?
    - Знаете, здесь такой не живет...
    - Вот, - улыбаясь, говорит профессор, - это всего лишь легкое удивление. Смотрите дальше.
    Набирает номер снова.
    - Здравствуйте, а Вася не подошел?
    - Сказал же, нет тут таких...
    Потирая руки, профессор заговорщицки подмигивает аудитории.
    - Клюнул. Ну, а теперь...
    Третий раз набирает номер.
    - Так Васи нет?
    - Да пошел ты...
    - Что ж, товарищи, надеюсь, пример вам понятен. Приступим к теоретической части...
    С первой парты встает молодой человек.
    - Простите, профессор, но вы забыли четвертую стадию.
    - Это какую же?
    - Стадию полного офигения.
    Подойдя к кафедре, молодой человек набирает номер.
    - Добрый день. Это Вася. Мне никто не звонил?
```	


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

Получить список подключеных устройств
```
{"ListDevice":"true"}
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



```html
<br/>
<br/>
<hr/>
<br/><input id="DeviceName" placeholder="Укажите имя устройства от которого будут отправлятся сообщения" style="width:90%"/>
<br/><input id="SendDeviceName" placeholder="Укажите имя устройства которому будет оставлено сообщение"  style="width:90%"/>
<br/><textarea id="MessageText" style="width:50%"></textarea>
<br/><input id="ResSend" placeholder="Результат отправки сообщения"  style="width:90%"/>
<br/><button onclick="pushMessage()">Отправить сообщение</button>
<br/>
<br/>
<hr/>
<hr/>
<hr/>
<br/>
<br/><input id="ReadDeviceName" placeholder="Укажите имя устройства от которого будем получать сообщение"  style="width:90%"/>
<br/><textarea id="MessageTextRead" style="width:50%"></textarea>
<br/><button onclick="popMessage()">Получить сообщение</button>
<br/>
<br/>
<br/>
<hr/>

<script>

  var pushMessage = function(){
        var DeviceName =  document.getElementById('ReadDeviceName').value ;
        var SendDeviceName =  document.getElementById('SendDeviceName').value ;
        var MessageText =  document.getElementById('MessageText').value ;
        var ResSend =  document.getElementById('ResSend') ;
		var sendObj = {"from": SendDeviceName,"MessageText":MessageText  }
		var xhr = new XMLHttpRequest()
		xhr.open( 'POST','http://128.0.24.172:8266/push='+SendDeviceName, true );
		xhr.onreadystatechange = function() {
		  if (xhr.readyState != 4) { return; }
		  if (xhr.status === 200) {
			console.log('result', xhr.responseText);
			ResSend.value=xhr.responseText;
		  } else {
			console.log('err', xhr.responseText);
			ResSend.value = xhr.responseText;
		  }
		}
		xhr.send(JSON.stringify(sendObj));
  }
  var popMessage = function(){
         var ReadDeviceName =  document.getElementById('ReadDeviceName').value ;
         var xhr = new XMLHttpRequest()
		xhr.open( 'GET','http://128.0.24.172:8266/pop='+ReadDeviceName, true )
		xhr.onreadystatechange = function() {
		  if (xhr.readyState != 4) { return; }
		  if (xhr.status === 200) {
		    document.getElementById('MessageTextRead').value = xhr.responseText;
			console.log('result', xhr.responseText)
		  } else {
			console.log('err', xhr.responseText)
		    document.getElementById('MessageTextRead').value = xhr.responseText;
		  }
		}
		xhr.send()
  }


</script>



```
