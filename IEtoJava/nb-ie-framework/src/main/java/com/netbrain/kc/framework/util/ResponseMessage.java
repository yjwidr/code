package com.netbrain.kc.framework.util;

public class ResponseMessage {
	private OperationResult operationResult;
	private Object data;
	public class OperationResult {
		private int resultCode;
		private Object resultDesc;
		public int getResultCode() {
			return resultCode;
		}
		public void setResultCode(int resultCode) {
			this.resultCode = resultCode;
		}
		public Object getResultDesc() {
			return resultDesc;
		}
		public void setResultDesc(Object resultDesc) {
			this.resultDesc = resultDesc;
		}
	}
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
    public OperationResult getOperationResult() {
		return operationResult;
	}

	public void setOperationResult(OperationResult operationResult) {
		this.operationResult = operationResult;
	}

//	public static void main(String[] args) {
//    	Demo demo = new Demo();
//    	ResponseMessage rm = new ResponseMessage();
//    	ResponseMessage.OperationResult or = rm.new OperationResult();
//    	demo.setAge(10);
//    	demo.setName("name2");
//    	demo.setId(20l);
//    	demo.setHeight(170.12d);
//    	demo.setWeight(109.21f);
//    	demo.setSaralry(new BigDecimal(21234.32));
//    	demo.setBirthday(new Date());
//    	or.setResultCode(0);
//    	or.setResultDesc("success");
//    	rm.setOperationResult(or);
//    	rm.setData(demo);
//    	String json=JSON.toJSONString(rm);
//    	System.out.println(json);
//    	//�������ʹfastjson������ת��integer,long,double���ֶε�ʱ��������ֶμ�ֵ���������
//    	json=JSON.toJSONString(rm,SerializerFeature.WriteDateUseDateFormat,SerializerFeature.WriteNonStringKeyAsString,SerializerFeature.WriteNonStringValueAsString);
//    	System.out.println(json);
//    	ResponseMessage rm2=JSON.parseObject(json, ResponseMessage.class); 
//		System.out.println(rm2.getOperationResult().getResultCode());
//	}
}
