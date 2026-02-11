package CLI.InputConfiguration.Operations;

import java.util.List;

public class Operation {

private List<String> operAnds;
private String operation;
private String id;
private boolean preCompute;
private double temp;

public String getId() { return id; }
public String getOperation() { return operation; }
public List<String> getOperAnds() { return operAnds; }
public boolean isPreCompute() { return preCompute; }
public double getTemp() { return temp; }

}
