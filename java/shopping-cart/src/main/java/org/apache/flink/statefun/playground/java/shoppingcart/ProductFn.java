package org.apache.flink.statefun.playground.java.shoppingcart;

import static org.apache.flink.statefun.playground.java.shoppingcart.Messages.SET_PRODUCT_TYPE;
import static org.apache.flink.statefun.playground.java.shoppingcart.Messages.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.flink.statefun.sdk.java.AddressScopedStorage;
import org.apache.flink.statefun.sdk.java.Context;
import org.apache.flink.statefun.sdk.java.StatefulFunction;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.ValueSpec;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.apache.flink.statefun.playground.java.shoppingcart.Messages.InventoryOperation;
import org.apache.flink.statefun.playground.java.shoppingcart.Messages.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ProductFn implements StatefulFunction {

	private static final Logger LOG = LoggerFactory.getLogger(UserShoppingCartFn.class);

	static final TypeName TYPE = TypeName.typeNameFromString("com.example/products");
	static final ValueSpec<ProductList> PRODUCTS = ValueSpec.named("products").withCustomType(ProductList.TYPE);
	@Override
	public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
		
		AddressScopedStorage storage = context.storage();
		
		 if (message.is(ADD_PRODUCT_TYPE)) {
		      Product product = message.as(ADD_PRODUCT_TYPE);

		      LOG.info("{}", product);
		      LOG.info("Scope: {}", context.self());
		      LOG.info("Caller: {}", context.caller());
		      
		      final ProductList products = storage.get(PRODUCTS).orElse(new ProductList());
		      
		      products.add(product.getId(),product);
		      
		      storage.set(PRODUCTS, products);
		      LOG.info("Products: {}", products);
		      return context.done();
		 } 
		
		return null;
	}
	
	private static class ProductList{
		private static final ObjectMapper mapper = new ObjectMapper();

	    public static final Type<ProductList> TYPE =
	        SimpleType.simpleImmutableTypeFrom(
	            TypeName.typeNameFromString("com.example/Products"),
	            mapper::writeValueAsBytes,
	            bytes -> mapper.readValue(bytes, ProductList.class));

	    
	    private final Map<String,Product> products;
	    
	    public ProductList() {
	    	products = new HashMap<String,Product>();
	    }
	    	    

	    public void add(String productId, Product product) {
	      products.put(productId, product);
	    }

	    public void delete(String productId) {
	      products.remove(productId);
	    }
	    
	    @Override
	    public String toString() {
	      return "Products{" + "products=" + products + '}';
	    }
	    
	}	

}
