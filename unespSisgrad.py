'''
Unesp Sisgrad Login Script
Enters Sisgrad (Unesp's system) and downloads messages

By LucasZanella.com
01/04/2016
'''

file = open('account.txt')
content = file.read().replace('user','').replace('password','').replace('=','').split()

user = content[0]
password = content[1]

import requests
from bs4 import BeautifulSoup


baseurl = 'https://sistemas.feg.unesp.br/sentinela/'
login_action = 'login.action'
login = baseurl+login_action
getMessagesAction = 'sentinela.openMessage.action?emailTipo=recebidas'
messages = baseurl+getMessagesAction
viewMessagesAction = 'sentinela.viewMessage.action?txt_id='"+msgID+"'&emailTipo=recebidas'
viewMessages = baseurl+viewMessagesAction


data = {'txt_usuario': user, 'txt_senha': password}

s = requests.Session()
r = s.post(login, data)
r = s.get(messages)
soup = BeautifulSoup(r.text, 'html.parser')


def removeBlankSpaceExcess(text):
  return(text.strip())

p=0
for line in soup.find_all('tr'):
  if (p!=0 and p<2):
    lineObjects = line.find_all('td')
    sentBy = removeBlankSpaceExcess(lineObjects[2].text)
    subject = removeBlankSpaceExcess(lineObjects[3].text)
    msgIDObject = lineObjects[3].a.get('href')
    msgIDstart = msgIDObject.find('(')
    msgIDend = msgIDObject.find(')')
    msgID = msgIDObject[msgIDstart+1:msgIDend].replace("'","")
    sentDate = removeBlankSpaceExcess(lineObjects[4].text)
    readDate = removeBlankSpaceExcess(lineObjects[5].text)
    msg = s.get("https://sistemas.feg.unesp.br/sentinela/sentinela.viewMessage.action?txt_id="+msgID+"&emailTipo=recebidas")
    print(msgID)
    #print("https://sistemas.feg.unesp.br/sentinela/sentinela.viewMessage.action?txt_id='"+msgID+"'&emailTipo=recebidas")
    print(msg.text)
  p+=1
  #break


