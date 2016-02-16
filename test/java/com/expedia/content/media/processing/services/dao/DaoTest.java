package com.expedia.content.media.processing.services.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:media-services.xml")
public class DaoTest {

    @Autowired
    private AmazonDynamoDBClient dynamoDBClient;

    private DynamoDB db;
    private DynamoDBMapper mapper;

    @Before
    public void initTest() {
        db = new DynamoDB(dynamoDBClient);
        mapper = new DynamoDBMapper(dynamoDBClient);
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
        Table table = db.getTable("cs-mediadb-Media");
        Index index = table.getIndex("cs-mediadb-index-Media-MediaFileName");

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("MediaFileName = :mfn")
                .withValueMap(new ValueMap()
                        .withString(":mfn","9462972_47_142288931795623.jpg"));

        ItemCollection<QueryOutcome> items = index.query(spec);
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next().toJSONPretty());
        }
    }

    @Test
    public void testWithMapper() {
        final HashMap<String, AttributeValue> params = new HashMap<>();
        params.put(":mfn", new AttributeValue().withS("8843978_13_000092fK.jpg"));

        DynamoDBQueryExpression<Media> expression = new DynamoDBQueryExpression<Media>()
                .withIndexName("cs-mediadb-index-Media-MediaFileName")
                .withConsistentRead(false)
                .withKeyConditionExpression("MediaFileName = :mfn")
                .withExpressionAttributeValues(params);

        List<Media> mediaList = mapper.query(Media.class, expression);

        assertNotNull(mediaList);
        assertEquals(1, mediaList.size());
        System.out.println(mediaList.get(0).toString());
        Media media = mediaList.get(0);
        assertEquals("650f13be-537f-4f09-bc68-6a69afef8766", media.getMediaGuid());
        assertEquals("8843978_13_000092fK.jpg", media.getFileName());
        assertEquals("Lodging", media.getDomain());
        assertEquals("8843978", media.getDomainId());
        assertEquals("true", media.getActive());
        assertEquals("int", media.getEnvironment());
        assertEquals("16364766", media.getLcmMediaId());
        assertNotNull(media.getLastUpdated());
    }
}
