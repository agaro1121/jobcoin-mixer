# A mixer for scrambling addresses on the Jobcoin network\

[Jobcoin](http://jobcoin.projecticeland.net/syllabize)

###Test Evidence:

1) You provide your addresses to jobcoin-mixer with in the following manner: 

```json
{
	"addresses": [
		{ "address": "myAddress13" },
		{ "address": "myAddress14" }
	]
}
```

2) jobcoin-mixer service will provide you a deposit address for your transaction like so:

```json
{
  "address":"31db076e-e018-4cec-896c-701934c46eee"
}
```

3) You create a transaction on Jobcoin with your newly assigned deposit address:
    - Address: 31db076e-e018-4cec-896c-701934c46eee
    - Amount: 1000
    
    ![Screenshot](/Picture1.png)
    
4 | 5) jobcoin-mixer polls for your transaction and sends it to it's house account:
```json
{
   "timestamp": "2017-03-11T02:56:01.776Z",
   "toAddress": "31db076e-e018-4cec-896c-701934c46eee", 
   "amount": "1000"
},
{
   "timestamp": "2017-03-11T02:56:10.150Z",
   "fromAddress": "31db076e-e018-4cec-896c-701934c46eee",
   "toAddress": "TestHouseAccount",
   "amount": "1000"
},
```

![Screenshot](/HouseTransactionScreenshot.png)

6) Then, over some time the mixer will use the house account to dole out your jobcoins in smaller increments to the withdrawal addresses you provided:
Example Logs:
```
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 500.0
    [info] a.TransactionActor - Amount to be doled out to each address = 250.0
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 250.00
    [info] a.TransactionActor - Amount to be doled out to each address = 125.00
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 125.000
    [info] a.TransactionActor - Amount to be doled out to each address = 62.500
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 62.5000
    [info] a.TransactionActor - Amount to be doled out to each address = 31.2500
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 31.25000
    [info] a.TransactionActor - Amount to be doled out to each address = 15.62500
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 15.625000
    [info] a.TransactionActor - Amount to be doled out to each address = 7.812500
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 7.8125000
    [info] a.TransactionActor - Amount to be doled out to each address = 3.9062500
    
    [info] a.TransactionActor - Deposit Address Address(31db076e-e018-4cec-896c-701934c46eee) has new balance = 0
    [info] a.TransactionActor - Amount to be doled out to each address = 3.9062500
    
    [info] a.TransactionActor - DepositAddress(Address(31db076e-e018-4cec-896c-701934c46eee)) has been fulfilled. Removing from pending transactions...
```

![Screenshot](/DoleOutTransactions.png)

```json
{
    "timestamp": "2017-03-11T02:56:15.012Z",
    "fromAddress": "TestHouseAccount",
    "toAddress": "myAddress13",
    "amount": "250.0"
  },
  {
    "timestamp": "2017-03-11T02:56:15.034Z",
    "fromAddress": "TestHouseAccount",
    "toAddress": "myAddress14",
    "amount": "250.0"
  },
  {
    "timestamp": "2017-03-11T02:56:34.995Z",
    "fromAddress": "TestHouseAccount",
    "toAddress": "myAddress14",
    "amount": "125.0"
  },
  {
    "timestamp": "2017-03-11T02:56:35.007Z",
    "fromAddress": "TestHouseAccount",
    "toAddress": "myAddress13",
    "amount": "125.0"
  },
  {
    "timestamp": "2017-03-11T02:56:54.996Z",
    "fromAddress": "TestHouseAccount",
    "toAddress": "myAddress14",
    "amount": "62.5"
  }
```