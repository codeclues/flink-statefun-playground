package org.ramslabs;

import static org.ramslabs.Messages.ITEM_AVAILABILITY_TYPE;
import static org.ramslabs.Messages.REQUEST_ITEM_TYPE;
import static org.ramslabs.Messages.RESTOCK_ITEM_TYPE;
import static org.ramslabs.Messages.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.flink.statefun.sdk.java.Address;
import org.apache.flink.statefun.sdk.java.AddressScopedStorage;
import org.apache.flink.statefun.sdk.java.Context;
import org.apache.flink.statefun.sdk.java.StatefulFunction;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.ValueSpec;
import org.apache.flink.statefun.sdk.java.message.Message;
import org.apache.flink.statefun.sdk.java.message.MessageBuilder;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;
import org.ramslabs.Messages.ItemAvailability;
import org.ramslabs.Messages.RequestItem;
import org.ramslabs.Messages.RestockItem;
import org.ramslabs.Messages.ItemAvailability.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

final class InventoryFn implements StatefulFunction {

	private static final Logger LOG = LoggerFactory.getLogger(UserShoppingCartFn.class);

	static final TypeName TYPE = TypeName.typeNameFromString("com.example/inventorydemo");
	static final ValueSpec<Inventory> INVENTORY = ValueSpec.named("inventory").withCustomType(Inventory.TYPE);
	
	@Override
	public CompletableFuture<Void> apply(Context context, Message message) throws Throwable {
	
		AddressScopedStorage storage = context.storage();

	    if (message.is(SET_PRODUCT_TYPE)) {
	      InventoryOperation setproduct = message.as(SET_PRODUCT_TYPE);

	      LOG.info("{}", setproduct);
	      LOG.info("Scope: {}", context.self());
	      LOG.info("Caller: {}", context.caller());
	      
	      final Inventory inventory = storage.get(INVENTORY).orElse(new Inventory());
	      
	      inventory.set(setproduct.getProductId(), setproduct.getQuantity());
	      
	      storage.set(INVENTORY, inventory);
	      LOG.info("Inventory: {}", inventory);
	      return context.done();
	    } 
	    return context.done();
	  }
	
	private static class Inventory {

	    private static final ObjectMapper mapper = new ObjectMapper();

	    public static final Type<Inventory> TYPE =
	        SimpleType.simpleImmutableTypeFrom(
	            TypeName.typeNameFromString("com.example/Inventory"),
	            mapper::writeValueAsBytes,
	            bytes -> mapper.readValue(bytes, Inventory.class));

	    
	    private final Map<String, Double> products;
	    
	    public Inventory() {
	    	products = new HashMap<String,Double>();
	    }
	    	    
	    public void set(String productId, double quantity) {
		  products.put(productId, quantity);
		}

	    public void increment(String productId, double quantity) {
	      products.put(productId, products.getOrDefault(productId, 0.0) + quantity);
	    }

	    public void decrement(String productId, double quantity) {
	      double remainder = products.getOrDefault(productId, 0.0) - quantity;
	      if (remainder > 0) {
	        products.put(productId, remainder);
	      } else {
	        products.remove(productId);
	      }
	    }
	    
	    @Override
	    public String toString() {
	      return "Inventory{" + "inventory=" + products + '}';
	    }
	}	
}


