package com.expedia.content.media.processing.services.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:media-services.xml")
public class DaoTest {

    @Autowired
    private AmazonDynamoDBClient dynamoDBClient;

    private DynamoDB db;

    @Before
    public void initTest() {
        db = new DynamoDB(dynamoDBClient);
    }

    @Test
    public void testLoadContext() {
        assertNotNull(dynamoDBClient);
    }

    @Test
    public void testQueryByGUID() {
        Table media = db.getTable("cs-mediadb-Media");

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("MediaGUID = :guid")
                .withValueMap(new ValueMap()
                        .withString(":guid", "094d4cb8-da8e-45c0-bcfb-3669eb15b36a"));

        ItemCollection<QueryOutcome> items = media.query(spec);

        Iterator<Item> iterator = items.iterator();
        Item item;
        while (iterator.hasNext()) {
            item = iterator.next();
            System.out.println(item.toJSONPretty());
        }
    }

    @Test
    public void testQueryBySha1() {
        /* To create the Secondary Index
        // Attribute definitions
        List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();

        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("SHA1")
                .withAttributeType("S"));
        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("ContentID")
                .withAttributeType("S"));
        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("MediaGUID")
                .withAttributeType("S"));

        // Table key schema
        ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<>();
        tableKeySchema.add(new KeySchemaElement()
                .withAttributeName("MediaGUID")
                .withKeyType(KeyType.HASH));  //Partition key

        // PrecipIndex
        GlobalSecondaryIndex sha1Index = new GlobalSecondaryIndex()
                .withIndexName("cs-mediadb-index-Media-SHA1_ContentID")
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits((long) 10)
                        .withWriteCapacityUnits((long) 1))
                .withProjection(new Projection().withProjectionType(ProjectionType.ALL));

        ArrayList<KeySchemaElement> indexKeySchema = new ArrayList<>();

        indexKeySchema.add(new KeySchemaElement()
                .withAttributeName("SHA1")
                .withKeyType(KeyType.HASH));  //Partition key
        indexKeySchema.add(new KeySchemaElement()
                .withAttributeName("ContentID")
                .withKeyType(KeyType.RANGE));  //Sort key

        sha1Index.setKeySchema(indexKeySchema);

        CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName("cs-mediadb-Media")
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits((long) 5)
                        .withWriteCapacityUnits((long) 1))
                .withAttributeDefinitions(attributeDefinitions)
                .withKeySchema(tableKeySchema)
                .withGlobalSecondaryIndexes(sha1Index);

        Table table = db.createTable(createTableRequest);
        System.out.println(table.getDescription());
        */
        Table table = db.getTable("cs-mediadb-Media");
        Index index = table.getIndex("cs-mediadb-index-Media-SHA1_ContentID");

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#s = :sha1")
                .withNameMap(new NameMap()
                        .with("#s", "String"))
                .withValueMap(new ValueMap()
                        .withString(":sha1","31B924EC3B066BDF0A79262CC04F10E5EE20156D"));

        ItemCollection<QueryOutcome> items = index.query(spec);
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next().toJSONPretty());
        }
    }
}
