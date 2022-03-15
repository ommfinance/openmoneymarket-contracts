package finance.omm.score;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.iconloop.score.test.Account;

import finance.omm.commons.Addresses;
import score.Address;
import score.Context;
import score.annotation.External;

public class MockOmmToken{

	private Map<Address, Account> accounts = new HashMap<>();
	private Account owner;

	@External
	public void setOwner(Account owner) {
		this.owner = owner;
	}

	@External
	public void addTo(Account to) {
		this.accounts.put(to.getAddress(), to);
	}

    @External(readonly=true)
    public String name() {
        return "Omm Token";
    }

    @External
    public void transfer(Address _to, BigInteger _value) {
    	owner.subtractBalance(Addresses.OMM_TOKEN, _value);
    	accounts.get(_to).addBalance(Addresses.OMM_TOKEN, _value);
    	Context.println(name() +"| transferred: " +  _value + " to: " + _to );
    }

}
