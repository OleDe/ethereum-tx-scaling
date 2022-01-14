package approach4.txcreation.config;

import approach2.txcreation.contracts.DappBackend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;

@Component
@Slf4j
public class Web3jConfiguration {

    private final AppProperties appProperties;

    private Credentials credentials;
    private Web3j web3jInstance;
    private DappBackend contract;
    private int interval = -1;
    private int contingentSize = -1;

    private final ContractGasProvider gasProvider = new StaticGasProvider(BigInteger.ONE, BigInteger.valueOf(999999999));

    @Autowired
    public Web3jConfiguration(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public Web3j getWeb3jInstance() {
        if(this.web3jInstance == null) {
            System.out.println("node url: " + this.appProperties.nodeUrl);
            WebSocketService socket = new WebSocketService(this.appProperties.nodeUrl, true);
            try {
                socket.connect();
                this.web3jInstance = Web3j.build(socket);
                log.info("connected to geth node {}, {}", this.appProperties.nodeUrl, this.web3jInstance.web3ClientVersion().send().getWeb3ClientVersion());
            } catch (IOException e) {
                log.error("connection to geth node failed", e);
                this.web3jInstance = null;
            }
        }
        return this.web3jInstance;
    }

    public Credentials getCredentials() {
        if(this.credentials == null) {
            this.credentials = Credentials.create(this.appProperties.privateKey);
            log.info("using account with address: {},\npublic key: {},\n private key: {}",
                    this.credentials.getAddress(),
                    this.credentials.getEcKeyPair().getPublicKey().toString(16),
                    this.credentials.getEcKeyPair().getPrivateKey().toString(16));
        }
        return this.credentials;
    }

    public Credentials setCredentials(String privateKey) {
        this.credentials = Credentials.create(privateKey);
        log.info("using account with address: {},\npublic key: {},\nprivate key: {}",
                this.credentials.getAddress(),
                this.credentials.getEcKeyPair().getPublicKey().toString(16),
                this.credentials.getEcKeyPair().getPrivateKey().toString(16));
        // update Contract
        if(this.contract != null) {
            this.contract = DappBackend.load(this.contract.getContractAddress(), this.getWeb3jInstance(), this.getCredentials(), this.gasProvider);
        }

        return this.credentials;
    }

    public String getSmartContractAddress() {
        if(this.contract == null) {
            this.contract = DappBackend.load(this.appProperties.contractAddress, this.getWeb3jInstance(), this.getCredentials(), this.gasProvider);
            log.info("created contract reference to contract at address: " + this.contract.getContractAddress());
        }
        return this.contract.getContractAddress();
    }

    public String setSmartContractAddress(String contractAddress) {
        this.contract = DappBackend.load(contractAddress, this.getWeb3jInstance(), this.getCredentials(), this.gasProvider);
        log.info("created contract reference to contract at address: " + this.contract.getContractAddress());
        return this.contract.getContractAddress();
    }

    public int getInterval() {
        if(this.interval == -1) {
            this.interval = this.appProperties.txInterval;
            log.info("tx creation interval: " + this.interval);
        }
        if(this.interval < 0) {
            throw new RuntimeException("tx interval can not be smaller than 0 ms");
        }
        return this.interval;
    }

    public int setInterval(int newInterval) {
        if(newInterval < 0) {
            throw new RuntimeException("tx interval can not be smaller than 0 ms");
        }
        this.interval = newInterval;
        log.info("tx creation interval: " + this.interval);
        return this.interval;
    }

    public int getContingentSize() {
        if(this.contingentSize == -1) {
            this.contingentSize = this.appProperties.contingentSize;
            log.info("nonce contingent size: " + this.contingentSize);
        }
        if(this.contingentSize < 1) {
            throw new RuntimeException("nonce contingent size can not be smaller than 1 ms");
        }
        return this.contingentSize;
    }

    public int setContingentSize(int newSize) {
        if(newSize < 1) {
            throw new RuntimeException("nonce contingent size can not be smaller than 1 ms");
        }
        this.contingentSize = newSize;
        log.info("nonce contingent size: " + this.contingentSize);
        return this.contingentSize;
    }

}
