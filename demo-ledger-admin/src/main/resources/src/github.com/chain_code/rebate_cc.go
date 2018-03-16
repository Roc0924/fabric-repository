/**
 * Create with GoLand
 * Author               : wangzhenpeng
 * Date                 : 2018/3/9
 * Time                 : 下午4:50
 * Description          : rebate chain code
 */
package main



import (
	"fmt"
	"strconv"
	"bytes"
	"encoding/json"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	pb "github.com/hyperledger/fabric/protos/peer"
)

// Rebate chaincode implementation
type RebateChaincode struct {
}

type RebateAccount struct{
	AccountId string
	Amount,ExpectAmount  int64
	Status string // normal,frozen,stop
	Details string
	Memo string
}

func (chaincode *RebateChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("Init chaincode rebate_cc")

	return shim.Success(nil)
}

func (chaincode *RebateChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	fmt.Println("call method Invoke")
	function, args := stub.GetFunctionAndParameters()
	if "invoke" != function {
		return shim.Error("unknown function invoke")
	}
	if args[0] == "createAccount" {
		// get one key's history records
		return chaincode.createAccount(stub,args)
	} else if args[0] == "queryAccount" {
		// get one key's history records
		return chaincode.queryAccount(stub,args)
	} else if args[0] == "deleteAccount" {
		// Deletes account from ledger state
		return chaincode.deleteAccount(stub, args)
	} else if args[0] == "queryHistory" {
		// get one key's history records
		return chaincode.queryHistory(stub,args)
	} else if args[0] == "createPlan" {
		// get one key's history records
		return chaincode.createPlan(stub,args)
	} else if args[0] == "queryPlan" {
		// the old "Query" is now implemtned in invoke
		return chaincode.queryPlan(stub, args)
	} else if args[0] == "addAmountFromBudget" {
		// get one key's history records
		return chaincode.addAmountFromBudget(stub,args)
	} else if args[0] == "rollBackAmountToBudget" {
		// get one key's history records
		return chaincode.rollBackAmountToBudget(stub,args)
	} else if args[0] == "addExpectAmountFromBudget" {
		// get one key's history records
		return chaincode.addExpectAmountFromBudget(stub,args)
	} else if args[0] == "rollBackExpectAmountToBudget" {
		// get one key's history records
		return chaincode.rollBackExpectAmountToBudget(stub,args)
	} else if args[0] == "collectExcept" {
		// get one key's history records
		return chaincode.collectExcept(stub,args)
	} /*else if function == "minusExpectAmount" {
		// get one key's history records
		return chaincode.minusExpectAmount(stub,args)
	}*/

	return shim.Error("Invalid invoke function name. Expecting 'createAccount'" +
		" 'queryAccount' 'deleteAccount' 'queryHistory' 'createPlan' 'queryPlan' 'addAmountFromBudget' 'query' 'query'")
}
func (chaincode *RebateChaincode) queryHistory(stub shim.ChaincodeStubInterface, args []string) pb.Response{
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}
	key:=args[1]
	it,err:= stub.GetHistoryForKey(key)
	if err!=nil{
		return shim.Error(err.Error())
	}
	var result,_= getHistoryListResult(it)
	return shim.Success(result)
}
func getHistoryListResult(resultsIterator shim.HistoryQueryIteratorInterface) ([]byte,error){

	defer resultsIterator.Close()
	// buffer is a JSON array containing QueryRecords
	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		queryResponse, err := resultsIterator.Next()
		if err != nil {
			return nil, err
		}
		// Add a comma before array members, suppress it for the first array member
		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}
		item,_:= json.Marshal( queryResponse)
		buffer.Write(item)
		//      buffer.Write(queryResponse)
		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")
	fmt.Printf("queryResult:\n%s\n", buffer.String())
	return buffer.Bytes(), nil
}


// create account in ledger
func (chaincode *RebateChaincode) createAccount(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	//var amount, expectAmount int64 // Asset holdings
	var err error
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}

	rebateAccountStr := args[1]
	var rebateAccount RebateAccount
	fmt.Println(rebateAccountStr);
	err = json.Unmarshal([]byte(rebateAccountStr),&rebateAccount)
	fmt.Printf("rebate account : accountId:%s, amount:%d, expectAmount:%d, status:%s, details:%s, memo:%s\n",
		rebateAccount.AccountId, rebateAccount.Amount, rebateAccount.ExpectAmount, rebateAccount.Status,
			rebateAccount.Details, rebateAccount.Memo)

	if nil != err {
		return shim.Error("unmarshal error :" + rebateAccountStr);
	}




	//account := args[1]
	//amount, err = strconv.ParseInt(args[2], 10, 64)
	//if err != nil {
	//	return shim.Error("Expecting integer value for asset holding")
	//}
	//expectAmount, err = strconv.ParseInt(args[2], 10, 64)
	//if err != nil {
	//	return shim.Error("Expecting integer value for asset holding")
	//}


	//whether the account has registered
	record, recordErr := stub.GetState(rebateAccount.AccountId)
	if nil != recordErr {
		return shim.Error("Inner Error" + recordErr.Error())
	}

	if nil != record {
		return shim.Error("Account has registered")
	}


	//rebateAccount := &RebateAccount{amount: amount, expectAmount: expectAmount,status:args[3],details:args[4],memo:args[5]}

	//byteObject,_ := json.Marshal(rebateAccount)
	// Put the key into the state in ledger
	putErr := stub.PutState(rebateAccount.AccountId,[]byte(rebateAccountStr))
	if putErr != nil {
		return shim.Error("Failed to put state, error:" + putErr.Error())
	}

	return shim.Success(nil)
}
// Deletes an entity from state
func (chaincode *RebateChaincode) createPlan(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3")
	}
	A := args[1]
	val := args[2]
	A ="plan_"+A
	// Delete the key from the state in ledger
	err := stub.PutState(A,[]byte(val))
	if err != nil {
		return shim.Error("Failed to delete state")
	}

	return shim.Success(nil)
}
// Deletes an entity from state
func (chaincode *RebateChaincode) deleteAccount(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}

	accountId := args[1]

	accountStateByte, getErr := stub.GetState(accountId)
	if nil != getErr {
		shim.Error("get state of " + accountId + " error:" + getErr.Error())
	}

	// Delete the key from the state in ledger
	err := stub.DelState(accountId)
	if err != nil {
		return shim.Error("Failed to delete state， error:" + err.Error())
	}

	return shim.Success(accountStateByte)
}

// query callback representing the query of a chaincode
func (chaincode *RebateChaincode) queryPlan(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var A string // Entities
	var err error

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting name of the person to query")
	}

	A = args[1]

	// Get the state from the ledger
	Avalbytes, err := stub.GetState("plan_"+A)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + A + "\"}"
		return shim.Error(jsonResp)
	}

	if Avalbytes == nil {
		jsonResp := "{\"Error\":\"Nil amount for " + A + "\"}"
		return shim.Error(jsonResp)
	}

	jsonResp := "{\"Name\":\"" + A + "\",\"Amount\":\"" + string(Avalbytes) + "\"}"
	fmt.Printf("Query Response:%s\n", jsonResp)
	return shim.Success(Avalbytes)
}
// query callback representing the query of a chaincode
func (chaincode *RebateChaincode) queryAccount(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	var accountId string // Entities
	var err error

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}

	accountId = args[1]

	// Get the state from the ledger
	recordbytes, err := stub.GetState(accountId)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + accountId + "\"}"
		return shim.Error(jsonResp)
	}

	if recordbytes == nil {
		jsonResp := "{\"Error\":\"Nil amount for " + accountId + "\"}"
		return shim.Error(jsonResp)
	}

	//jsonResp := "{\"Name\":\"" + accountId + "\",\"Amount\":\"" + string(recordbytes) + "\"}"
	fmt.Printf("Query Response:%s\n", recordbytes)
	return shim.Success(recordbytes)
}

// query callback representing the query of a chaincode
func (chaincode *RebateChaincode) addAmountFromBudget(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}

	planId := "plan_" + args[1]
	accountId := args[2]
	delta, err := strconv.ParseInt(args[3], 10, 64)
	details := args[4]
	if nil != err {
		shim.Error("parse delta error:" + err.Error())
	}
	return chaincode.moveBudgetToAccount(stub,planId, accountId, delta, "amount", details)
}


func (chaincode *RebateChaincode) addExpectAmountFromBudget(stub shim.ChaincodeStubInterface, args []string) pb.Response {
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}

	planId := "plan_" + args[1]
	accountId := args[2]
	delta, err := strconv.ParseInt(args[3], 10, 64)
	details := args[4]
	if nil != err {
		shim.Error("parse delta error:" + err.Error())
	}
	return chaincode.moveBudgetToAccount(stub,planId, accountId, delta, "expect", details)
}

func (chaincode *RebateChaincode) rollBackAmountToBudget(stub shim.ChaincodeStubInterface, args []string) pb.Response {


	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}
	planId := "plan_" + args[1]
	accountId := args[2]
	delta, err := strconv.ParseInt(args[3], 10, 64)
	details := args[4]
	if nil != err {
		shim.Error("parse delta error:" + err.Error())
	}

	return chaincode.moveAccountToBudget(stub, accountId, planId, delta, "amount", details)
}



func (chaincode *RebateChaincode) rollBackExpectAmountToBudget(stub shim.ChaincodeStubInterface, args []string) pb.Response {


	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}
	planId := "plan_" + args[1]
	accountId := args[2]
	delta, err := strconv.ParseInt(args[3], 10, 64)
	details := args[4]
	if nil != err {
		shim.Error("parse delta error:" + err.Error())
	}

	return chaincode.moveAccountToBudget(stub, accountId, planId, delta, "expect", details)
}



func (chaincode *RebateChaincode) moveBudgetToAccount(stub shim.ChaincodeStubInterface,
	source string, destination string, delta int64, moveType string, details string) pb.Response {

	// Get the state from the ledger
	budgetVal, err := stub.GetState(source)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + source + "\"}"
		return shim.Error(jsonResp)
	}
	accountVal, err := stub.GetState(destination)
	if err != nil{
		jsonResp :="{\"Error\":\"get account "+destination +" err \"}"
		return shim.Error(jsonResp)
	}
	account := RebateAccount{}
	err = json.Unmarshal(accountVal,&account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+destination +" unmarshal err \"}"
		return shim.Error(jsonResp)
	}

	fmt.Printf(string(budgetVal) + "\n")
	budget,err := strconv.ParseInt(string(budgetVal), 10, 64)
	if err != nil {
		jsonResp := "{\"Error\":\"budget is not int \"}"
		return shim.Error(jsonResp)
	}



	budget = budget - delta
	if budget < 0 {
		jsonResp := "{\"Error\":\"budget is not enough \"}"
		return shim.Error(jsonResp)
	}
	err = stub.PutState(source,[]byte(strconv.FormatInt(budget, 10)))
	if err != nil{
		return shim.Error(err.Error())
	}

	if "expect" == moveType {
		account.ExpectAmount = account.ExpectAmount + delta
	} else {
		account.Amount = account.Amount + delta
	}
	account.Details = details
	accountByte,err := json.Marshal(account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+destination +" format err \"}"
		return shim.Error(jsonResp)
	}
	err = stub.PutState(destination,accountByte)
	if err != nil{
		return shim.Error(err.Error())
	}
	return shim.Success(nil)
}




func (chaincode *RebateChaincode) moveAccountToBudget(stub shim.ChaincodeStubInterface, source string,
	destination string, delta int64, moveType string, details string) pb.Response {



	// Get the state from the ledger
	accountVal, err := stub.GetState(source)
	if err != nil{
		jsonResp :="{\"Error\":\"get account "+source +" err \"}"
		return shim.Error(jsonResp)
	}
	budgetVal, err := stub.GetState(destination)
	if err != nil {
		jsonResp := "{\"Error\":\"Failed to get state for " + destination + "\"}"
		return shim.Error(jsonResp)
	}
	account := RebateAccount{}
	err = json.Unmarshal(accountVal,&account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+source +" unmarshal err \"}"
		return shim.Error(jsonResp)
	}

	budget,err := strconv.ParseInt(string(budgetVal), 10, 64)
	if err != nil {
		jsonResp := "{\"Error\":\"budget is not int \"}"
		return shim.Error(jsonResp)
	}

	if "expect" == moveType {
		account.ExpectAmount = account.ExpectAmount - delta
		if account.ExpectAmount < 0 {
			jsonResp := "{\"Error\":\"expect amount is not enough \"}"
			return shim.Error(jsonResp)
		}
	} else if "amount" == moveType {
		account.Amount = account.Amount - delta
		if account.Amount < 0 {
			jsonResp := "{\"Error\":\"amount is not enough \"}"
			return shim.Error(jsonResp)
		}
	}



	account.Details = details
	accountByte,err := json.Marshal(account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+ source +" format err \"}"
		return shim.Error(jsonResp)
	}
	err = stub.PutState(source,accountByte)
	if err != nil{
		return shim.Error(err.Error())
	}





	budget = budget + delta

	err = stub.PutState(destination,[]byte(strconv.FormatInt(budget, 10)))
	if err != nil{
		return shim.Error(err.Error())
	}

	return shim.Success(nil)
}



func (chaincode *RebateChaincode) collectExcept(stub shim.ChaincodeStubInterface, args[] string) pb.Response {

	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments. Expecting 4")
	}

	accountId := args[1]
	delta, err:= strconv.ParseInt(args[2], 10, 64)
	if nil != err {
		shim.Error("parse delta error: " + err.Error())
	}

	details := args[3]

	// Get the state from the ledger
	accountVal, err := stub.GetState(accountId)
	if err != nil{
		jsonResp :="{\"Error\":\"get account "+accountId +" err \"}"
		return shim.Error(jsonResp)
	}
	account := RebateAccount{}
	err = json.Unmarshal(accountVal,&account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+accountId +" unmarshal err \"}"
		return shim.Error(jsonResp)
	}


	account.ExpectAmount = account.ExpectAmount - delta
	if account.ExpectAmount < 0 {
		jsonResp := "{\"Error\":\"expect amount is not enough \"}"
		return shim.Error(jsonResp)
	}
	account.Details = details
	account.Amount = account.Amount + delta
	accountByte,err := json.Marshal(account)
	if err != nil{
		jsonResp :="{\"Error\":\"account "+ accountId +" format err \"}"
		return shim.Error(jsonResp)
	}
	err = stub.PutState(accountId,accountByte)
	if err != nil{
		return shim.Error(err.Error())
	}
	return shim.Success(nil)
}



func main() {
	err := shim.Start(new(RebateChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}
}
